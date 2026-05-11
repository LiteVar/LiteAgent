package com.litevar.agent.rest.agentflow.listener;

import cn.hutool.core.util.ObjectUtil;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.rest.agentflow.event.AgentEvent;
import com.litevar.agent.rest.agentflow.event.AgentEventListener;
import com.litevar.agent.rest.agentflow.message.LlmEvent;
import com.litevar.agent.rest.executor.StoreMessageExecutor;

/**
 * 持久化消息
 *
 * @author uncle
 * @since 2025/12/19 12:22
 */
public record StoreMsgEventListener() implements AgentEventListener {
    @Override
    public void onEvent(AgentEvent event) {
        switch (event.getPayload().type()) {
            case USER_SEND_EVENT -> userSendMsg(event);
            case LLM_EVENT -> llmMsg(event);
            case THINK_EVENT -> thinkMsg(event);
            case ERROR_EVENT -> errorMsg(event);
            case FUNCTION_CALL_EVENT -> functionCallMsg(event);
            case TOOL_RESULT_EVENT -> functionResultMsg(event);
            case REFLECTION_EVENT -> reflectMsg(event);
            case AGENT_DISPATCH_EVENT -> distributeMsg(event);
            case KNOWLEDGE_EVENT -> knowledgeMsg(event);
            case AGENT_SWITCH_EVENT -> switchAgentMsg(event);
            case PLANNING_EVENT -> planningMsg(event);
            default -> {
            }
        }
    }

    private void userSendMsg(AgentEvent event) {
        OutMessage outMessage = transformSendMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void llmMsg(AgentEvent event) {
        LlmEvent msg = (LlmEvent) event.getPayload();
        if (ObjectUtil.notEqual(msg.agentType(), AgentType.REFLECTION.getType())) {
            OutMessage outMessage = transformLlmMsg(event);
            StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
        }
    }

    private void thinkMsg(AgentEvent event) {
        OutMessage outMessage = transformThinkMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void errorMsg(AgentEvent event) {
        OutMessage outMessage = transformErrorMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void functionCallMsg(AgentEvent event) {
        OutMessage outMessage = transformFunctionCallMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void functionResultMsg(AgentEvent event) {
        OutMessage outMessage = transformToolResultMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void reflectMsg(AgentEvent event) {
        OutMessage outMessage = transformReflectMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void distributeMsg(AgentEvent event) {
        OutMessage outMessage = transformDistributeMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void knowledgeMsg(AgentEvent event) {
        OutMessage outMessage = transformKnowledgeMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void switchAgentMsg(AgentEvent event) {
        OutMessage outMessage = transformAgentSwitchMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }

    private void planningMsg(AgentEvent event) {
        OutMessage outMessage = transformPlanningMsg(event);
        StoreMessageExecutor.store(event.getSessionId(), event.getRequestId(), outMessage);
    }
}
