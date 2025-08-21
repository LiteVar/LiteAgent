package com.litevar.agent.rest.openai.agent;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.dto.ReflectMessageInfo;
import com.litevar.agent.base.dto.ReflectToolINfo;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.TaskStatus;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ReflectResult;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.base.vo.SegmentVO;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.openai.OpenAiChatModel;
import com.litevar.agent.openai.completion.ChatContext;
import com.litevar.agent.openai.completion.ChatModelRequest;
import com.litevar.agent.openai.completion.CompletionCallback;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.ToolMessage;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.openai.message.*;
import com.litevar.agent.rest.service.SegmentService;
import com.litevar.agent.rest.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.litevar.agent.rest.util.FunctionUtil.*;

/**
 * @author uncle
 * @since 2025/3/5 10:19
 */
@Data
@Slf4j
public class MultiAgent {
    private final String contextId;
    private final ChatModelRequest request;

    private final String agentId;
    private String agentName;
    private Integer agentType;
    private Integer executeMode;
    private List<String> sequence;
    private List<String> datasetIds;

    private Map<String, MultiAgent> reflectAgentMap = new HashMap<>();
    private Map<String, MultiAgent> generalAgentMap = new HashMap<>();
    private Map<String, MultiAgent> distributeAgentMap = new HashMap<>();

    private static ChatContext chatContext;

    public MultiAgent(String agentId, ChatModelRequest request) {
        this.request = request;
        this.contextId = request.getContextId();
        this.agentId = agentId;
    }

    public void generate(CurrentAgentRequest.AgentRequest currentContext, List<Message> messages, boolean stream) {
        log.info("[stream={},taskId={}]消息:{}", stream, currentContext.getTaskId(), messages);
        CompletionCallback callback = createCompletionCallback(currentContext);

        if (stream) {
            OpenAiChatModel.generate(request, currentContext.getTaskId(), messages, callback);
        } else {
            try {
                callback.start(currentContext.getTaskId());
                CompletionResponse response = OpenAiChatModel.generate(request, currentContext.getTaskId(), messages);
                callback.onCompleteResponse(currentContext.getTaskId(), response, false);
            } catch (Exception ex) {
                callback.onError(currentContext.getTaskId(), ex);
            }
        }
    }

    private CompletionCallback createCompletionCallback(CurrentAgentRequest.AgentRequest currentContext) {
        return new CompletionCallback() {
            @Override
            public void onPartialResponse(String taskId, String part, Integer chunkType) {
                //有反思agent不能直接输出,要等反思通过才能输出
                //屏蔽反思agent的输出:{information:xxx,score:10}
                if (chunkType == 1 || (ObjectUtil.notEqual(agentType, AgentType.REFLECTION.getType()) && ObjectUtil.isEmpty(reflectAgentMap))) {
                    AgentManager.handleMsg(AgentMsgType.CHUNK_MSG, new ChunkMessage(currentContext, chunkType, part));
                }
            }

            @Override
            public void onError(String taskId, Throwable error) {
                handleError(currentContext, error);
            }

            @Override
            public void onCompleteResponse(String taskId, CompletionResponse response, boolean isStream) {
                CurrentAgentRequest.restore(currentContext);
                handleCompleteResponse(taskId, response, isStream);
            }

            @Override
            public void start(String taskId) {
                TaskStatusManager.update(taskId, TaskStatus.CALLING_LLM);
            }
        };
    }

    /**
     * 处理完整响应
     */
    private void handleCompleteResponse(String taskId, CompletionResponse response, boolean isStream) {
        //有思考内容的话,先保存
        AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();
        if (StrUtil.isNotBlank(assistantMessage.getReasoningContent().trim())) {
            AgentManager.handleMsg(AgentMsgType.THINK_MSG, new LlmMessage(agentType, response));
            assistantMessage.setReasoningContent(null);
        }

        if (response.isFunctionCalling()) {

            if (StrUtil.isNotBlank(assistantMessage.getContent())) {
                //兼容:function-calling有文本输出的情况
                CompletionResponse lr = JSONUtil.toBean(JSONUtil.toJsonStr(response), CompletionResponse.class);
                lr.setUsage(null);
                AgentManager.handleMsg(AgentMsgType.LLM_MSG, new LlmMessage(agentType, lr));
            }

            List<Message> result = handleFunctionCalling(response, isStream);
            if (!result.isEmpty()) {
                //function-calling,将结果返回给大模型
                CurrentAgentRequest.AgentRequest context = CurrentAgentRequest.getContext();
                CurrentAgentRequest.clear();
                generate(context, result, isStream);
            } else {
                //agent分发、agent规划的情况,不需要返回,本次task结束
                taskDone();
                getChatContext().taskDone(contextId, taskId);
            }
        } else {
            handleNormalResponse(response, isStream, taskId);
        }
    }

    /**
     * 处理普通响应
     * 处理非函数调用的普通大模型响应，包括反思逻辑
     */
    private void handleNormalResponse(CompletionResponse response, boolean isStream, String taskId) {
        AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();

        List<ReflectResult> reflectResultList = handleLLMReflect(response, isStream);
        List<Message> submitMsg = reflectResultList.stream().filter(i -> i.getScore() >= 0)
                .map(i -> (Message) UserMessage.of(i.getInformation())).toList();

        if (!submitMsg.isEmpty()) {
            CurrentAgentRequest.AgentRequest context = CurrentAgentRequest.getContext();
            CurrentAgentRequest.clear();
            generate(context, submitMsg, isStream);
        } else {

            //反思通过或不用再反思了(次数达到限制)
            if (ObjectUtil.isNotEmpty(reflectAgentMap) && isStream) {
                //有反思,需要补充delta输出
                TypewriterEffectUtil.part(assistantMessage.getContent(), 50, part ->
                        AgentManager.handleMsg(AgentMsgType.CHUNK_MSG, new ChunkMessage(0, part)));
            }
            AgentManager.handleMsg(AgentMsgType.LLM_MSG, new LlmMessage(agentType, response));

            //把task上下文加入总的上下文
            getChatContext().taskDone(request.getContextId(), taskId);

            //将结果广播给普通agent
            broadcastAgent(assistantMessage.getContent(), isStream);

            //本次任务完成
            taskDone();
        }
    }

    private void taskDone() {
        TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.COMPLETED);

        AgentManager.handleMsg(AgentMsgType.TASK_DONE, new TaskDoneMessage());
        CurrentAgentRequest.clear();
    }

    public void handleError(CurrentAgentRequest.AgentRequest currentContext, Throwable error) {
        CurrentAgentRequest.restore(currentContext);
        TaskStatusManager.update(currentContext.getTaskId(), TaskStatus.FAILED);

        if (ObjectUtil.notEqual(agentType, AgentType.REFLECTION.getType())) {
            ErrorMessage errorMessage = new ErrorMessage(currentContext, error);
            AgentManager.handleMsg(AgentMsgType.ERROR_MSG, errorMessage);
        }

        taskDone();
    }

    /**
     * function-calling
     */
    private List<Message> handleFunctionCalling(CompletionResponse response, boolean isStream) {
        CompletionResponse cResponse = JSONUtil.toBean(JSONUtil.toJsonStr(response), CompletionResponse.class);
        AssistantMessage fcMsg = cResponse.getChoices().get(0).getMessage();

        List<Message> resultMsg = handleNotFunction(fcMsg.getToolCalls(), isStream);
        fcMsg.getToolCalls().removeIf(i -> StrUtil.equalsAny(i.getFunction().getName(),
                TOOL_KNOWLEDGE_BASE, TOOL_AGENT_DISTRIBUTE, TOOL_AUTO_AGENT));

        if (fcMsg.hasToolCalls()) {
            TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.CALLING_FUNCTION);
            AgentManager.handleMsg(AgentMsgType.FUNCTION_CALL_MSG, new LlmMessage(agentType, cResponse));
            for (AssistantMessage.ToolCall toolCall : fcMsg.getToolCalls()) {
                ToolExecutionResult executionResult = executeToolCall(toolCall);

                if (executionResult.shouldBreak()) {
                    resultMsg.add(UserMessage.of(executionResult.getContent()));
                    break;
                }

                String callId = toolCall.getId();
                String result = executionResult.getContent();

                String functionId = getFunctionId(toolCall.getFunction().getName());
                AgentManager.handleMsg(AgentMsgType.FUNCTION_RESULT_MSG, new ToolResultMessage(callId, result, functionId));

                ToolMessage toolMessage = new ToolMessage();
                toolMessage.setToolCallId(callId);
                toolMessage.setContent(result);
                resultMsg.add(toolMessage);
            }
        }

        return finalizeResultMessages(response, resultMsg);
    }

    private ToolExecutionResult executeToolCall(AssistantMessage.ToolCall toolCall) {
        String callId = toolCall.getId();
        String arguments = toolCall.getFunction().getArguments();
        String functionId = FunctionUtil.getFunctionId(toolCall.getFunction().getName());

        // Check if functionId is valid and function exists
        ToolFunction function = null;
        if (StrUtil.isNotBlank(functionId)) {
            try {
                function = SpringUtil.getBean(ToolFunctionService.class).findById(functionId);
            } catch (Exception e) {
            }
        }

        if (function == null) {
            String errorMsg = "function does not exist: " + toolCall.getFunction().getName();
            log.warn("[sessionId={},taskId={}] {}", CurrentAgentRequest.getSessionId(), CurrentAgentRequest.getTaskId(), errorMsg);
            return new ToolExecutionResult(errorMsg, false);
        }

        Dict reflectRes = toolReflect(functionId);
        if (reflectRes.getBool("canExecute")) {
            String result = FunctionUtil.callFunction(callId, contextId, function, arguments);
            return new ToolExecutionResult(result, false);
        } else {
            return handleReflectionFailure(reflectRes);
        }
    }

    private ToolExecutionResult handleReflectionFailure(Dict reflectRes) {
        String content = reflectRes.getStr("content");
        log.info("[sessionId={},taskId={},工具序列反思没通过]返回消息:{}",
                CurrentAgentRequest.getSessionId(), CurrentAgentRequest.getTaskId(), content);

        String type = reflectRes.getStr("type");
        boolean shouldBreak = StrUtil.isNotEmpty(type) && StrUtil.equals(type, "methodWrong");

        return new ToolExecutionResult(content, shouldBreak);
    }

    private List<Message> finalizeResultMessages(CompletionResponse response, List<Message> resultMsg) {
        List<String> toolCallIds = resultMsg.stream()
                .filter(i -> i instanceof ToolMessage)
                .map(i -> ((ToolMessage) i).getToolCallId())
                .toList();

        if (toolCallIds.isEmpty()) {
            return resultMsg;
        }

        AssistantMessage originMsg = response.getChoices().get(0).getMessage();
        originMsg.getToolCalls().removeIf(toolCall -> !toolCallIds.contains(toolCall.getId()));

        resultMsg.add(0, originMsg);

        boolean isAllAgentDistribute = originMsg.getToolCalls().stream()
                .allMatch(i -> StrUtil.equals(i.getFunction().getName(), TOOL_AGENT_DISTRIBUTE));
        if (isAllAgentDistribute) {
            getChatContext().addTaskMessage(contextId, CurrentAgentRequest.getTaskId(), resultMsg);
            return Collections.emptyList();
        }

        return resultMsg;
    }

    /**
     * 工具执行结果
     * 封装工具执行的结果和是否需要中断后续处理
     */
    private static class ToolExecutionResult {
        private final String content;
        private final boolean shouldBreak;

        public ToolExecutionResult(String content, boolean shouldBreak) {
            this.content = content;
            this.shouldBreak = shouldBreak;
        }

        public String getContent() {
            return content;
        }

        public boolean shouldBreak() {
            return shouldBreak;
        }
    }

    private List<Message> handleNotFunction(List<AssistantMessage.ToolCall> toolCallList, boolean isStream) {
        List<AssistantMessage.ToolCall> notFunctionList = toolCallList.stream()
                .filter(toolCall -> StrUtil.equalsAny(toolCall.getFunction().getName(), TOOL_KNOWLEDGE_BASE, TOOL_AGENT_DISTRIBUTE, TOOL_AUTO_AGENT))
                .toList();
        List<Message> resultMsg = new ArrayList<>();
        if (!notFunctionList.isEmpty()) {
            for (AssistantMessage.ToolCall toolCall : notFunctionList) {
                String result = "";
                JSONObject argObj = JSONUtil.parseObj(toolCall.getFunction().getArguments());
                if (StrUtil.equals(toolCall.getFunction().getName(), TOOL_KNOWLEDGE_BASE)) {
                    String content = argObj.getStr("content");

                    TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.RETRIEVING_KNOWLEDGE);

                    result = knowledgeBase(content);

                } else if (StrUtil.equals(toolCall.getFunction().getName(), TOOL_AGENT_DISTRIBUTE)) {
                    String cmd = argObj.getStr("cmd");
                    String targetAgentId = argObj.getStr("agentId");

                    TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.CALLING_SUB_AGENT);

                    result = distributeAgent(targetAgentId, cmd, isStream);
                } else if (StrUtil.equals(toolCall.getFunction().getName(), TOOL_AUTO_AGENT)) {
                    //auto agent
                    String taskContent = argObj.getStr("task");
                    AgentUtil agentUtil = SpringUtil.getBean(AgentUtil.class);

                    TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.CALLING_SUB_AGENT);

                    List<AgentPlanningDTO> planList = agentUtil.planning(this, isStream, taskContent);
                    if (ObjectUtil.isNotEmpty(planList)) {
                        String planId = IdUtil.getSnowflakeNextIdStr();
                        RedisUtil.setValue(String.format(CacheKey.SESSION_PLAN_INFO, planId), planList, 30, TimeUnit.MINUTES);
                        AgentManager.handleMsg(AgentMsgType.PLANNING_MSG, new PlanningMessage(planId, planList));
                    } else {
                        CurrentAgentRequest.clear();
                        throw new ServiceException("agent规划过程出错");
                    }
                }

                if (StrUtil.isNotEmpty(result)) {
                    ToolMessage toolMessage = new ToolMessage();
                    toolMessage.setToolCallId(toolCall.getId());
                    toolMessage.setContent(result);
                    resultMsg.add(toolMessage);
                }
            }
        }
        return resultMsg;
    }

    /**
     * 知识库检索
     */
    private String knowledgeBase(String content) {
        String result = "";
        log.info("[sessionId={},taskId={},知识库检索内容:{}", CurrentAgentRequest.getSessionId(), CurrentAgentRequest.getTaskId(), content);
        List<OutMessage.KnowledgeHistoryInfo> history = Collections.emptyList();
        try {
            Dict dict = SpringUtil.getBean(SegmentService.class).retrieve(CurrentAgentRequest.getAgentId(), datasetIds, content);
            List<SegmentVO> segment = (List<SegmentVO>) dict.get("result");
            history = (List<OutMessage.KnowledgeHistoryInfo>) dict.get("history");
            if (ObjectUtil.isNotEmpty(segment)) {
                result = segment.parallelStream().map(SegmentVO::getContent).collect(Collectors.joining("\n"));
            } else {
                result = "the knowledge base retrieve results is empty";
                history = Collections.emptyList();
            }

        } catch (Exception ex) {
            result = "knowledge base retrieve failed";
        }
        log.info("[sessionId={},taskId={},知识库检索结果:{}", CurrentAgentRequest.getSessionId(), CurrentAgentRequest.getTaskId(), result);

        AgentManager.handleMsg(AgentMsgType.KNOWLEDGE_MSG, new KnowledgeMessage(content, history));
        return result;
    }

    /**
     * agent 分发
     */
    private String distributeAgent(String targetAgentId, String cmd, boolean isStream) {
        //agent分发
        MultiAgent agent = distributeAgentMap.get(targetAgentId);
        if (agent == null) {
            log.error("agentId={},agent不存在", targetAgentId);
            return null;
        }

        //切换到子agent
        String taskId = IdUtil.getSnowflakeNextIdStr();
        AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG, new AgentSwitchMessage(agent.getAgentId(), agent.getAgentName(), taskId));

        //分发指令
        AgentManager.handleMsg(AgentMsgType.DISTRIBUTE_MSG, new DistributeMessage(cmd, agent.getAgentId(), taskId));

        Map<String, CompletionResponse> map = AgentManager.chat(agent, taskId, List.of(UserMessage.of(cmd)), isStream);

        String result;
        if (ObjectUtil.isNotEmpty(map) && StrUtil.isNotBlank(map.get(agent.getAgentId()).getChoices().get(0).getMessage().getContent())) {
            result = map.get(agent.getAgentId()).getChoices().get(0).getMessage().getContent();
        } else {
            result = "agent:" + agent.getAgentName() + "调用失败";
        }
        return result;
    }

    /**
     * 将agent输出广播给所有子agent
     */
    private void broadcastAgent(String output, boolean isStream) {
        if (ObjectUtil.isNotEmpty(generalAgentMap)) {
            TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.CALLING_SUB_AGENT);
            List<Message> submitMsg = List.of(UserMessage.of(output));

            generalAgentMap.forEach((agentId, agent) -> {
                String taskId = IdUtil.getSnowflakeNextIdStr();
                //切换到子agent
                AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG, new AgentSwitchMessage(agentId, agent.getAgentName(), taskId));

                AgentManager.handleMsg(AgentMsgType.DISTRIBUTE_MSG, new DistributeMessage(output, agentId, taskId));

                AgentManager.chat(agent, taskId, submitMsg, isStream);
            });
        }
    }

    /**
     * 对大模型输出进行反思
     */
    private List<ReflectResult> handleLLMReflect(CompletionResponse llmOutput, boolean isStream) {
        if (ObjectUtil.isNotEmpty(reflectAgentMap)) {
            String outputContent = llmOutput.getChoices().get(0).getMessage().getContent();
            ReflectMessageInfo reflectInfo = (ReflectMessageInfo) RedisUtil.getValue(String.format(CacheKey.REFLECT_INFO, CurrentAgentRequest.getTaskId()));
            reflectInfo.setReflectCount(reflectInfo.getReflectCount() + 1);

            TaskStatusManager.update(CurrentAgentRequest.getTaskId(), TaskStatus.CALLING_SUB_AGENT);

            log.info("taskId={},第{}次反思", CurrentAgentRequest.getTaskId(), reflectInfo.getReflectCount());

            String reflectInputMsg = String.format("\"rawInput\":\"%s\",\"rawOutput\":\"%s\"",
                    reflectInfo.getInput(), outputContent);
            List<ReflectResult> reflectOutput = reflect(isStream, List.of(UserMessage.of(reflectInputMsg)));

            ReflectResultMessage rm = new ReflectResultMessage(this.agentName, reflectInfo.getInput(), outputContent, reflectOutput);
            AgentManager.handleMsg(AgentMsgType.REFLECT_MSG, rm);

            //分数小于等于7时,不通过. 所有反思agent通过才视为通过
            if (reflectOutput.stream().anyMatch(i -> i.getScore() <= 7)) {
                //保存最高分数对应的主agent输出
                ReflectResult maxScore = reflectOutput.stream()
                        .max(Comparator.comparingDouble(ReflectResult::getScore))
                        .get();
                if (maxScore.getScore() > reflectInfo.getScore()) {
                    reflectInfo.setScore(maxScore.getScore());
                    reflectInfo.setOutput(outputContent);
                }

                //反思不通过: 判断反思次数是否达到上限,如果上限了,取最高分数的记录作为输出
                if (reflectInfo.getReflectCount() == 10) {
                    llmOutput.getChoices().get(0).getMessage().setContent(reflectInfo.getOutput());
                    log.info("taskId={},反思次数达到上限,取最高分数的记录作为输出", CurrentAgentRequest.getTaskId());

                } else {
                    //继续反思
                    log.info("taskId={},第{}次反思不通过,继续反思", CurrentAgentRequest.getTaskId(), reflectInfo.getReflectCount());
                    RedisUtil.setValue(String.format(CacheKey.REFLECT_INFO, CurrentAgentRequest.getTaskId()), reflectInfo, 10, TimeUnit.MINUTES);
                    return reflectOutput;
                }

            } else {
                //所有agent反思通过
            }
            RedisUtil.delKey(String.format(CacheKey.REFLECT_INFO, CurrentAgentRequest.getTaskId()));
        }

        return Collections.emptyList();
    }

    private List<ReflectResult> reflect(boolean isStream, List<Message> reflectMsg) {
        List<ReflectResult> resultList = new ArrayList<>();
        reflectAgentMap.forEach((agentId, agent) -> {
            String taskId = IdUtil.getSnowflakeNextIdStr();
            AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG, new AgentSwitchMessage(agentId, agent.getAgentName(), taskId));
            Map<String, CompletionResponse> map = AgentManager.chat(agent, taskId, reflectMsg, isStream);
            if (ObjectUtil.isNotEmpty(map)) {
                CompletionResponse res = map.get(agentId);
                ReflectResult reflectResult = JSONUtil.toBean(res.getChoices().get(0).getMessage().getContent(), ReflectResult.class);
                resultList.add(reflectResult);
            } else {
                log.error("反思获取结果为null,taskId={}", taskId);
                ReflectResult reflectResult = new ReflectResult();
                reflectResult.setScore(-0.5d);
                reflectResult.setInformation("反思agent调用失败");
                resultList.add(reflectResult);
            }
        });
        return resultList;
    }

    /**
     * 工具序列反思
     */
    private Dict toolReflect(String functionId) {
        String taskId = CurrentAgentRequest.getTaskId();
        boolean canExecute = false;
        String content = "";
        String type = "";
        if (sequence.contains(functionId)) {
            Object value = RedisUtil.getValue(String.format(CacheKey.REFLECT_TOOL_INFO, agentId));
            ReflectToolINfo info;
            if (value == null) {
                //第一次调用
                info = new ReflectToolINfo();
                info.setTaskId(taskId);
                info.setSequence(new ArrayList<>(sequence));
                RedisUtil.setValue(String.format(CacheKey.REFLECT_TOOL_INFO, agentId), info, 10, TimeUnit.MINUTES);
            } else {
                info = (ReflectToolINfo) value;
            }

            if (StrUtil.isNotEmpty(info.getTaskId()) && !StrUtil.equals(taskId, info.getTaskId())) {
                //别的task正在执行序列,返回设备繁忙
                content = "The device is busy, please wait!";

            } else {
                //锁
                info.setTaskId(taskId);
                RedisUtil.setValue(String.format(CacheKey.REFLECT_TOOL_INFO, agentId), info, 10, TimeUnit.MINUTES);

                if (ObjectUtil.isNotEmpty(info.getSequence())) {
                    //当前应返回的functionId
                    String shouldFunctionId = info.getSequence().get(0);
                    if (StrUtil.equals(functionId, shouldFunctionId)) {
                        //与序列一致,反思通过
                        //移除掉该functionId
                        info.getSequence().remove(0);
                        info.setCount(0);
                        canExecute = true;

                    } else {
                        //不一致,提示需要返回的function
                        //反思失败次数+1
                        info.setCount(info.getCount() + 1);

                        if (info.getCount() > 10) {
                            //最多反思10次,不执行了
                            content = "too many executions,stop!";
                        } else {
                            //提示下一步要执行哪个方法
                            ToolFunction function = SpringUtil.getBean(ToolFunctionService.class).findById(shouldFunctionId);
                            content = "The method that needs to be returned next time is:"
                                    + FunctionUtil.generateFunctionName(function.getResource(), shouldFunctionId);
                            type = "methodWrong";
                        }
                    }
                    if (ObjectUtil.isEmpty(info.getSequence())) {
                        //序列里的方法都执行完了
                        RedisUtil.delKey(String.format(CacheKey.REFLECT_TOOL_INFO, agentId));
                    } else {
                        info.setTaskId(null);
                        RedisUtil.setValue(String.format(CacheKey.REFLECT_TOOL_INFO, agentId), info, 10, TimeUnit.MINUTES);
                    }
                }
            }

        } else {
            canExecute = true;
        }

        return Dict.create().set("canExecute", canExecute)
                .set("content", content)
                .set("type", type);
    }

    private ChatContext getChatContext() {
        if (chatContext == null) {
            chatContext = SpringUtil.getBean(ChatContext.class);
        }
        return chatContext;
    }
}
