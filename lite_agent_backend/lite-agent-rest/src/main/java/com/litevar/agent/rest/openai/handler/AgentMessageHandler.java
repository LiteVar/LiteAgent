package com.litevar.agent.rest.openai.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.rest.openai.message.*;
import com.litevar.agent.rest.util.FunctionUtil;
import com.litevar.agent.rest.util.SpringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息处理
 *
 * @author uncle
 * @since 2024/11/18 16:58
 */
public class AgentMessageHandler {

    /**
     * 用户发送的消息
     */
    public void onSend(UserSendMessage userSendMessage) {

    }

    /**
     * 大模型返回文本消息
     */
    public void LlmMsg(LlmMessage llmMessage) {

    }

    /**
     * 大模型思考内容
     */
    public void thinkMsg(LlmMessage llmMessage) {

    }

    /**
     * 异常信息
     */
    public void onError(ErrorMessage errorMessage) {

    }

    /**
     * stream 消息片段
     */
    public void chunk(ChunkMessage chunkMessage) {

    }

    /**
     * function-calling消息
     */
    public void functionCalling(LlmMessage functionCallingMessage) {

    }

    /**
     * open tool call消息
     */
    public void openToolCall(OpenToolMessage openToolMessage) {

    }

    /**
     * 函数调用结果
     */
    public void functionResult(ToolResultMessage toolResultMessage) {

    }

    /**
     * 反思
     */
    public void reflect(ReflectResultMessage reflectResultMessage) {

    }

    /**
     * agent分发消息
     */
    public void distribute(DistributeMessage distributeMessage) {

    }

    /**
     * 知识库调用
     */
    public void knowledge(KnowledgeMessage knowledgeMessage) {

    }

    /**
     * agent切换
     */
    public void agentSwitch(AgentSwitchMessage agentSwitchMessage) {

    }

    /**
     * 任务完成
     */
    public void taskDone(String agentId, String taskId) {

    }

    /**
     * 规划内容
     */
    public void planning(PlanningMessage planningMessage) {

    }

    public void disconnect(String requestId) {

    }

    protected OutMessage transformSendMessage(UserSendMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("user");
        outMessage.setType("text");
        outMessage.setContent(msg.getMessageList().get(0).getMessage());
        outMessage.setAgentId(msg.getAgentId());
        return outMessage;
    }

    protected OutMessage transformLlmMessage(LlmMessage msg) {
        OutMessage outMessage = llmMessage(msg);
        outMessage.setType("text");
        AssistantMessage assistantMessage = msg.getResponse().getChoices().get(0).getMessage();
        outMessage.setContent(assistantMessage.getContent());
        return outMessage;
    }

    protected OutMessage transformThinkMessage(LlmMessage msg) {
        OutMessage outMessage = llmMessage(msg);
        outMessage.setType("think");
        AssistantMessage assistantMessage = msg.getResponse().getChoices().get(0).getMessage();
        outMessage.setContent(assistantMessage.getReasoningContent());
        outMessage.setTokenUsage(null);
        return outMessage;
    }

    protected OutMessage transformErrorMessage(ErrorMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("error");
        Throwable ex = msg.getEx();
        String content = "暂时无法连接到服务器，请稍后再试";
        if (ex != null) {
            if (ex instanceof ServiceException) {
                content = ex.getMessage();
            } else if (StrUtil.isNotEmpty(ex.getMessage())
                    && ex.getMessage().contains("This model's maximum context length is")) {
                content = "上下文长度超过token限制，请刷新当前界面";
            }
        }
        outMessage.setContent(content);
        return outMessage;
    }

    protected OutMessage transformFunctionCallMessage(LlmMessage msg) {
        OutMessage outMessage = llmMessage(msg);
        outMessage.setType("functionCallList");
        AssistantMessage assistantMessage = msg.getResponse().getChoices().get(0).getMessage();
        List<OutMessage.FunctionCall> callList = new ArrayList<>();
        assistantMessage.getToolCalls().forEach(i -> {
            OutMessage.FunctionCall functionCall = new OutMessage.FunctionCall();
            functionCall.setId(i.getId());
            functionCall.setName(i.getFunction().getName());
            functionCall.setArguments(i.getFunction().getArguments());
            String functionId = FunctionUtil.getFunctionId(i.getFunction().getName());
            if (StrUtil.isNotBlank(functionId)) {
                try {
                    ToolFunction function = SpringUtil.getBean(ToolFunctionService.class).findById(functionId);
                    ToolProvider tool = SpringUtil.getBean(ToolService.class).findById(function.getToolId());
                    functionCall.setToolId(function.getToolId());
                    functionCall.setToolName(tool.getName());
                    functionCall.setFunctionName(function.getResource());
                } catch (Exception e) {
                }
            }
            functionCall.setFunctionId(functionId);

            callList.add(functionCall);
        });
        outMessage.setToolCalls(callList);
        return outMessage;
    }

    private OutMessage llmMessage(LlmMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("assistant");
        outMessage.setAgentId(msg.getAgentId());
        CompletionResponse response = msg.getResponse();
        outMessage.setId(response.getId());
        if (response.getUsage() != null) {
            outMessage.setTokenUsage(BeanUtil.copyProperties(response.getUsage(), OutMessage.TokenUsage.class));
        }
        return outMessage;
    }

    protected OutMessage transformToolResultMessage(ToolResultMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("tool");
        outMessage.setType("toolReturn");
        outMessage.setContent(msg.getResult());
        outMessage.setToolCallId(msg.getCallId());
        return outMessage;
    }

    protected OutMessage transformDistributeMessage(DistributeMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("dispatch");
        OutMessage.DistributeContent content = new OutMessage.DistributeContent();
        content.setTargetAgentId(msg.getTargetAgentId());
        content.setCmd(msg.getCmd());
        content.setDispatchId(msg.getDispatchId());
        outMessage.setContent(content);
        return outMessage;
    }

    protected OutMessage transformReflectMessage(ReflectResultMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("reflection");
        outMessage.setType("reflect");
        OutMessage.ReflectContent content = new OutMessage.ReflectContent();

        content.setRawInput(msg.getRawInput());
        content.setRawOutput(msg.getRawOutput());
        content.setOutput(msg.getReflectOutput());
        outMessage.setContent(content);
        return outMessage;
    }

    protected OutMessage transformAgentSwitchMessage(AgentSwitchMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("agentSwitch");
        OutMessage.AgentSwitchContent content = new OutMessage.AgentSwitchContent();
        content.setAgentName(msg.getAgentName());
        outMessage.setContent(content);
        return outMessage;
    }

    protected OutMessage transformKnowledgeMessage(KnowledgeMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("knowledge");
        OutMessage.KnowledgeContent content = new OutMessage.KnowledgeContent();
        content.setRetrieveContent(msg.getRetrieveContent());
        content.setInfo(msg.getHistoryInfo());
        outMessage.setContent(content);
        return outMessage;
    }

    protected OutMessage transformPlanningMessage(PlanningMessage msg) {
        OutMessage outMessage = new OutMessage();
        outMessage.setAgentId(msg.getAgentId());
        outMessage.setParentTaskId(msg.getParentTaskId());
        outMessage.setTaskId(msg.getTaskId());
        outMessage.setRole("agent");
        outMessage.setType("planning");
        OutMessage.PlanningContent content = new OutMessage.PlanningContent();
        content.setPlanId(msg.getPlanId());
        content.setTaskList(msg.getTaskList());
        outMessage.setContent(content);
        return outMessage;
    }
}