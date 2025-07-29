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
import com.litevar.agent.rest.util.AgentUtil;
import com.litevar.agent.rest.util.FunctionUtil;
import com.litevar.agent.rest.util.SpringUtil;
import com.litevar.agent.rest.util.TypewriterEffectUtil;
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
    private final String sessionId;
    private final String contextId;
    private final ChatModelRequest request;
    private final CompletionCallback callback;

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

    public MultiAgent(String agentId, String sessionId, ChatModelRequest request) {
        this.sessionId = sessionId;
        this.request = request;
        this.contextId = request.getContextId();
        this.agentId = agentId;
        this.callback = new CompletionCallback() {
            @Override
            public void onPartialResponse(String taskId, String part, Integer chunkType) {
                //有反思agent不能直接输出,要等反思通过才能输出
                //屏蔽反思agent的输出:{information:xxx,score:10}
                if (chunkType == 1 ||
                        (ObjectUtil.notEqual(agentType, AgentType.REFLECTION.getType()) && ObjectUtil.isEmpty(reflectAgentMap))) {
                    ChunkMessage chunkMessage = new ChunkMessage(sessionId, taskId, agentId, chunkType, part);
                    AgentManager.handleMsg(AgentMsgType.CHUNK_MSG, chunkMessage);
                }
            }

            @Override
            public void onError(String taskId, Throwable error) {
                if (ObjectUtil.notEqual(agentType, AgentType.REFLECTION.getType())) {
                    ErrorMessage errorMessage = new ErrorMessage(sessionId, taskId, agentId, error);
                    AgentManager.handleMsg(AgentMsgType.ERROR_MSG, errorMessage);
                }

                taskDone(taskId);
            }

            @Override
            public void onCompleteResponse(String taskId, CompletionResponse response, boolean isStream) {
                AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();
                if (StrUtil.isNotBlank(assistantMessage.getReasoningContent())) {
                    LlmMessage lm = new LlmMessage(sessionId, taskId, agentId, agentType, response);
                    AgentManager.handleMsg(AgentMsgType.THINK_MSG, lm);
                }

                if (response.isFunctionCalling()) {
                    if (StrUtil.isNotBlank(assistantMessage.getContent())) {
                        //兼容:function-calling有文本输出的情况
                        CompletionResponse lr = JSONUtil.toBean(JSONUtil.toJsonStr(response), CompletionResponse.class);
                        lr.setUsage(null);
                        LlmMessage lm = new LlmMessage(sessionId, taskId, agentId, agentType, lr);
                        AgentManager.handleMsg(AgentMsgType.LLM_MSG, lm);
                    }

                    List<Message> result = handleFunctionCalling(taskId, response, isStream);
                    if (!result.isEmpty()) {
                        //function-calling,将结果返回给大模型
                        generate(taskId, result, isStream);
                    } else {
                        //agent分发的情况,不需要返回,本次task结束
                        taskDone(taskId);
                        getChatContext().taskDone(contextId, taskId);
                    }

                } else {
                    List<ReflectResult> reflectResultList = handleLLMReflect(taskId, response, isStream);
                    List<Message> submitMsg = reflectResultList.stream().filter(i -> i.getScore() >= 0)
                            .map(i -> (Message) UserMessage.of(i.getInformation())).toList();
                    if (submitMsg.isEmpty()) {
                        //反思通过或不用再反思了(次数达到限制)
                        if (ObjectUtil.isNotEmpty(reflectAgentMap) && isStream) {
                            //有反思,需要补充delta输出
                            TypewriterEffectUtil.part(assistantMessage.getContent(), 50, part -> {
                                ChunkMessage chunkMessage = new ChunkMessage(sessionId, taskId, agentId, 0, part);
                                AgentManager.handleMsg(AgentMsgType.CHUNK_MSG, chunkMessage);
                            });
                        }
                        LlmMessage lm = new LlmMessage(sessionId, taskId, agentId, agentType, response);
                        AgentManager.handleMsg(AgentMsgType.LLM_MSG, lm);

                        //把task上下文加入总的上下文
                        getChatContext().taskDone(request.getContextId(), taskId);

                        //将结果广播给普通agent
                        broadcastAgent(taskId, assistantMessage.getContent(), isStream);

                        //本次任务完成
                        taskDone(taskId);

                    } else {
                        generate(taskId, submitMsg, isStream);
                    }
                }
            }

            @Override
            public void start(String taskId) {

            }
        };
    }

    private void taskDone(String taskId) {
        AgentManager.handleMsg(AgentMsgType.TASK_DONE, new TaskDoneMessage(sessionId, agentId, taskId));
    }

    public void generate(String taskId, List<Message> messages, boolean stream) {
        log.info("[stream={}]开始请求大模型:{}", stream, messages);
        if (stream) {
            OpenAiChatModel.generate(request, taskId, messages, callback);
        } else {
            try {
                CompletionResponse response = OpenAiChatModel.generate(request, taskId, messages);
                callback.onCompleteResponse(taskId, response, false);
            } catch (Exception ex) {
                callback.onError(taskId, ex);
            }
        }
    }

    /**
     * function-calling
     * 工具调用逻辑
     */
    private List<Message> handleFunctionCalling(String taskId, CompletionResponse response, boolean isStream) {
        CompletionResponse cResponse = JSONUtil.toBean(JSONUtil.toJsonStr(response), CompletionResponse.class);
        AssistantMessage fcMsg = cResponse.getChoices().get(0).getMessage();
        List<Message> notFunctionResult = handleNotFunction(taskId, fcMsg.getToolCalls(), isStream);
        fcMsg.getToolCalls().removeIf(i -> StrUtil.equalsAny(i.getFunction().getName(),
                TOOL_KNOWLEDGE_BASE, TOOL_AGENT_DISTRIBUTE, TOOL_AUTO_AGENT));

        List<Message> resultMsg = new ArrayList<>();
        resultMsg.addAll(notFunctionResult);

        if (fcMsg.hasToolCalls()) {
            //function-calling
            LlmMessage fcm = new LlmMessage(sessionId, taskId, agentId, agentType, cResponse);
            AgentManager.handleMsg(AgentMsgType.FUNCTION_CALL_MSG, fcm);

            for (AssistantMessage.ToolCall toolCall : fcMsg.getToolCalls()) {
                String callId = toolCall.getId();
                String result = "";
                String arguments = toolCall.getFunction().getArguments();
                String functionId = FunctionUtil.getFunctionId(toolCall.getFunction().getName());
                Dict reflectRes = toolReflect(taskId, functionId);
                if (reflectRes.getBool("canExecute")) {
                    result = FunctionUtil.callFunction(sessionId, taskId, callId, agentId, contextId, functionId, arguments);

                } else {
                    //序列反思没通过
                    String content = reflectRes.getStr("content");
                    log.info("[sessionId={},taskId={},工具序列反思没通过]返回消息:{}", sessionId, taskId, content);
                    String type = reflectRes.getStr("type");
                    if (StrUtil.isNotEmpty(type) && StrUtil.equals(type, "methodWrong")) {
                        //返回一条UserMessage提示LLM下一次要调用的function
                        resultMsg.add(UserMessage.of(content));
                        break;
                    } else {
                        result = content;
                    }
                }
                ToolResultMessage tc = new ToolResultMessage(sessionId, taskId, agentId, callId, result, toolCall.getFunction().getName());
                AgentManager.handleMsg(AgentMsgType.FUNCTION_RESULT_MSG, tc);

                ToolMessage toolMessage = new ToolMessage();
                toolMessage.setToolCallId(callId);
                toolMessage.setContent(result);

                resultMsg.add(toolMessage);
            }
        }

        List<String> toolCallIds = resultMsg.stream()
                .filter(i -> i instanceof ToolMessage)
                .map(i -> ((ToolMessage) i).getToolCallId()).toList();

        AssistantMessage originMsg = response.getChoices().get(0).getMessage();
        originMsg.getToolCalls().removeIf(toolCall -> !toolCallIds.contains(toolCall.getId()));

        if (!toolCallIds.isEmpty()) {
            resultMsg.add(0, originMsg);
            //如果function-calling消息都是agent分发消息,不直接返回给LLM,先加入上下文
            if (originMsg.getToolCalls().stream().allMatch(i -> StrUtil.equals(i.getFunction().getName(), TOOL_AGENT_DISTRIBUTE))) {
                getChatContext().addTaskMessage(contextId, taskId, resultMsg);
                return Collections.emptyList();
            }
        }
        return resultMsg;
    }

    private List<Message> handleNotFunction(String taskId, List<AssistantMessage.ToolCall> toolCallList, boolean isStream) {
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
                    result = knowledgeBase(taskId, content);

                } else if (StrUtil.equals(toolCall.getFunction().getName(), TOOL_AGENT_DISTRIBUTE)) {
                    String cmd = argObj.getStr("cmd");
                    String targetAgentId = argObj.getStr("agentId");

                    result = distributeAgent(taskId, targetAgentId, cmd, isStream);
                } else if (StrUtil.equals(toolCall.getFunction().getName(), TOOL_AUTO_AGENT)) {
                    //auto agent
                    String taskContent = argObj.getStr("task");
                    AgentUtil agentUtil = SpringUtil.getBean(AgentUtil.class);
                    List<AgentPlanningDTO> planList = agentUtil.planning(this, taskId, isStream, taskContent);
                    if (ObjectUtil.isNotEmpty(planList)) {
                        String planId = IdUtil.getSnowflakeNextIdStr();
                        RedisUtil.setValue(String.format(CacheKey.SESSION_PLAN_INFO, planId), planList, 30, TimeUnit.MINUTES);
                        AgentManager.handleMsg(AgentMsgType.PLANNING_MSG,
                                new PlanningMessage(sessionId, taskId, agentId, planList, planId));
                    } else {
                        //todo 规划过程出错
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
    private String knowledgeBase(String taskId, String content) {
        String result = "";
        log.info("[sessionId={},taskId={},知识库检索内容:{}", sessionId, taskId, content);
        List<OutMessage.KnowledgeHistoryInfo> history = Collections.emptyList();
        try {
            Dict dict = SpringUtil.getBean(SegmentService.class).retrieve(this.agentId, datasetIds, content);
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
        log.info("[sessionId={},taskId={},知识库检索结果:{}", sessionId, taskId, result);

        KnowledgeMessage km = new KnowledgeMessage(sessionId, taskId, agentId, content, history);
        AgentManager.handleMsg(AgentMsgType.KNOWLEDGE_MSG, km);
        return result;
    }

    /**
     * agent 分发
     */
    private String distributeAgent(String taskId, String targetAgentId, String cmd, boolean isStream) {
        //agent分发
        MultiAgent agent = distributeAgentMap.get(targetAgentId);
        if (agent == null) {
            log.error("agentId={},agent不存在", targetAgentId);
            return null;
        }

        DistributeMessage dm = new DistributeMessage(sessionId, taskId, this.agentId, cmd, agent.getAgentId(),
                IdUtil.getSnowflakeNextIdStr());
        AgentManager.handleMsg(AgentMsgType.DISTRIBUTE_MSG, dm);
        //切换到子agent
        AgentSwitchMessage as = new AgentSwitchMessage(sessionId, taskId, agent.getAgentId(), agent.getAgentName());
        AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG, as);

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
    private void broadcastAgent(String taskId, String output, boolean isStream) {
        if (ObjectUtil.isNotEmpty(generalAgentMap)) {
            List<Message> submitMsg = List.of(UserMessage.of(output));

            DistributeMessage bm = new DistributeMessage(sessionId, taskId, this.agentId, output, "",
                    IdUtil.getSnowflakeNextIdStr());
            AgentManager.handleMsg(AgentMsgType.BROADCAST_MSG, bm);

            generalAgentMap.forEach((agentId, agent) -> {
                //切换到子agent
                AgentSwitchMessage as = new AgentSwitchMessage(sessionId, taskId, agentId, agent.getAgentName());
                AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG, as);

                AgentManager.chat(agent, taskId, submitMsg, isStream);
            });
        }
    }

    /**
     * 对大模型输出进行反思
     */
    private List<ReflectResult> handleLLMReflect(String taskId, CompletionResponse llmOutput, boolean isStream) {
        if (ObjectUtil.isNotEmpty(reflectAgentMap)) {
            String outputContent = llmOutput.getChoices().get(0).getMessage().getContent();
            ReflectMessageInfo reflectInfo = (ReflectMessageInfo) RedisUtil.getValue(String.format(CacheKey.REFLECT_INFO, taskId));
            reflectInfo.setReflectCount(reflectInfo.getReflectCount() + 1);
            log.info("taskId={},第{}次反思", taskId, reflectInfo.getReflectCount());

            String reflectInputMsg = String.format("\"rawInput\":\"%s\",\"rawOutput\":\"%s\"",
                    reflectInfo.getInput(), outputContent);
            List<ReflectResult> reflectOutput = reflect(taskId, isStream, List.of(UserMessage.of(reflectInputMsg)));

            ReflectResultMessage rm = new ReflectResultMessage(sessionId, taskId, this.agentId, this.agentName,
                    reflectInfo.getInput(), outputContent, reflectOutput);
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
                    log.info("taskId={},反思次数达到上限,取最高分数的记录作为输出", taskId);

                } else {
                    //继续反思
                    log.info("taskId={},第{}次反思不通过,继续反思", taskId, reflectInfo.getReflectCount());
                    RedisUtil.setValue(String.format(CacheKey.REFLECT_INFO, taskId), reflectInfo, 10, TimeUnit.MINUTES);
                    return reflectOutput;
                }

            } else {
                //所有agent反思通过
            }
            RedisUtil.delKey(String.format(CacheKey.REFLECT_INFO, taskId));
        }

        return Collections.emptyList();
    }

    private List<ReflectResult> reflect(String taskId, boolean isStream, List<Message> reflectMsg) {
        List<ReflectResult> resultList = new ArrayList<>();
        reflectAgentMap.forEach((agentId, agent) -> {
            AgentSwitchMessage as = new AgentSwitchMessage(sessionId, taskId, agentId, agent.getAgentName());
            AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG, as);
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
    private Dict toolReflect(String taskId, String functionId) {
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
