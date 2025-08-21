package com.litevar.agent.rest.openai.handler;

import cn.hutool.core.util.ObjectUtil;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.rest.openai.executor.StoreMessageExecutor;
import com.litevar.agent.rest.openai.message.*;

import java.util.List;

/**
 * 消息持久化处理
 *
 * @author uncle
 * @since 2024/11/19 10:10
 */
public class StoreMessageHandler extends AgentMessageHandler {
    private final String sessionId;

    public StoreMessageHandler(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onSend(UserSendMessage userSendMessage) {
        OutMessage outMessage = transformSendMessage(userSendMessage);
        StoreMessageExecutor.store(sessionId, userSendMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void LlmMsg(LlmMessage llmMessage) {
        if (ObjectUtil.notEqual(llmMessage.getAgentType(), AgentType.REFLECTION.getType())) {
            OutMessage outMessage = transformLlmMessage(llmMessage);
            StoreMessageExecutor.store(sessionId, llmMessage.getRequestId(), List.of(outMessage));
        }
    }

    @Override
    public void thinkMsg(LlmMessage llmMessage) {
        OutMessage outMessage = transformThinkMessage(llmMessage);
        StoreMessageExecutor.store(sessionId, llmMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        OutMessage outMessage = transformErrorMessage(errorMessage);
        StoreMessageExecutor.store(sessionId, errorMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void functionCalling(LlmMessage functionCallingMessage) {
        OutMessage outMessage = transformFunctionCallMessage(functionCallingMessage);
        StoreMessageExecutor.store(sessionId, functionCallingMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void functionResult(ToolResultMessage toolResultMessage) {
        OutMessage outMessage = transformToolResultMessage(toolResultMessage);
        StoreMessageExecutor.store(sessionId, toolResultMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void reflect(ReflectResultMessage reflectResultMessage) {
        OutMessage outMessage = transformReflectMessage(reflectResultMessage);
        StoreMessageExecutor.store(sessionId, reflectResultMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void distribute(DistributeMessage distributeMessage) {
        OutMessage outMessage = transformDistributeMessage(distributeMessage);
        StoreMessageExecutor.store(sessionId, distributeMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void knowledge(KnowledgeMessage knowledgeMessage) {
        OutMessage outMessage = transformKnowledgeMessage(knowledgeMessage);
        StoreMessageExecutor.store(sessionId, knowledgeMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void agentSwitch(AgentSwitchMessage agentSwitchMessage) {
        OutMessage outMessage = transformAgentSwitchMessage(agentSwitchMessage);
        StoreMessageExecutor.store(sessionId, agentSwitchMessage.getRequestId(), List.of(outMessage));
    }

    @Override
    public void planning(PlanningMessage planningMessage) {
        OutMessage outMessage = transformPlanningMessage(planningMessage);
        StoreMessageExecutor.store(sessionId, planningMessage.getRequestId(), List.of(outMessage));
    }
}
