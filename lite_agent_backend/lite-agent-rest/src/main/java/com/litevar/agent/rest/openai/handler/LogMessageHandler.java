package com.litevar.agent.rest.openai.handler;

import cn.hutool.json.JSONUtil;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.rest.openai.message.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志处理
 *
 * @author uncle
 * @since 2024/11/18 17:47
 */
@Slf4j
public class LogMessageHandler extends AgentMessageHandler {
    private final String sessionId;

    public LogMessageHandler(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onSend(UserSendMessage userSendMessage) {
        log.info("[sessionId={},taskId={},agentId={}]发送消息:{}", sessionId, userSendMessage.getTaskId(),
                userSendMessage.getAgentId(), userSendMessage.getMessageList());
    }

    @Override
    public void LlmMsg(LlmMessage llmMessage) {
        AssistantMessage message = llmMessage.getResponse().getChoices().get(0).getMessage();
        log.info("[sessionId={},taskId={},agentId={}]LLM回复内容:{}", sessionId, llmMessage.getTaskId(),
                llmMessage.getAgentId(), message.getContent());
    }

    @Override
    public void thinkMsg(LlmMessage llmMessage) {
        String reasoningContent = llmMessage.getResponse().getChoices().get(0).getMessage().getReasoningContent();
        log.info("[sessionId={},taskId={},agentId={}]LLM思考内容:{}", sessionId, llmMessage.getTaskId(),
                llmMessage.getAgentId(), reasoningContent);
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        log.error("[sessionId={},taskId={},agentId={}]异常", sessionId, errorMessage.getTaskId(),
                errorMessage.getAgentId(), errorMessage.getEx());
    }

    @Override
    public void functionCalling(LlmMessage functionCallingMessage) {
        log.info("[sessionId={},taskId={},agentId={}]function-calling:{}", sessionId, functionCallingMessage.getTaskId(),
                functionCallingMessage.getAgentId(), JSONUtil.toJsonStr(functionCallingMessage.getResponse().getChoices().get(0).getMessage().getToolCalls()));
    }

    @Override
    public void functionResult(ToolResultMessage toolResultMessage) {
        log.info("[sessionId={},taskId={},agentId={}]tool调用返回结果:{}", sessionId, toolResultMessage.getTaskId(),
                toolResultMessage.getAgentId(), toolResultMessage.getResult());
    }

    @Override
    public void reflect(ReflectResultMessage reflectResultMessage) {
        log.info("[sessionId={},taskId={},agentId={}]【反思内容】:{}, 【结果】:{}", sessionId, reflectResultMessage.getTaskId(),
                reflectResultMessage.getAgentId(), reflectResultMessage.getRawInput() + "/n" + reflectResultMessage.getRawOutput(),
                JSONUtil.toJsonStr(reflectResultMessage.getReflectOutput()));
    }

    @Override
    public void distribute(DistributeMessage distributeMessage) {
        log.info("[sessionId={},taskId={},agentId={}]agent分发,目标agent:{},内容:{}", sessionId, distributeMessage.getTaskId(),
                distributeMessage.getAgentId(), distributeMessage.getTargetAgentId(), distributeMessage.getCmd());
    }

    @Override
    public void broadcast(DistributeMessage distributeMessage) {
        log.info("[sessionId={},taskId={},agentId={}]普通agent分发,目标agent:{},内容:{}", sessionId, distributeMessage.getTaskId(),
                distributeMessage.getAgentId(), distributeMessage.getTargetAgentId(), distributeMessage.getCmd());
    }

    @Override
    public void planning(PlanningMessage planningMessage) {
        log.info("[sessionId={},taskId={},agentId={}]agent规划,planId={},内容:{}", sessionId, planningMessage.getTaskId(),
                planningMessage.getAgentId(), planningMessage.getPlanId(), planningMessage.getTaskList());
    }
}
