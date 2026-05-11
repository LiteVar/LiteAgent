package com.litevar.agent.rest.agentflow.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.openai.ObjectMapperSingleton;
import com.litevar.agent.rest.agentflow.event.AgentEvent;
import com.litevar.agent.rest.agentflow.event.AgentEventListener;
import com.litevar.agent.rest.agentflow.message.ErrorEvent;
import com.litevar.agent.rest.agentflow.message.LlmEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 负责打印日志
 *
 * @author uncle
 * @since 2025/12/18 16:44
 */
@Slf4j
public record LogAgentEventListener() implements AgentEventListener {
    @Override
    public void onEvent(AgentEvent event) {
        switch (event.getPayload().type()) {
            //这些类型的消息不打印日志
            case CHUNK_EVENT, KNOWLEDGE_EVENT, AGENT_SWITCH_EVENT -> {
            }
            case THINK_EVENT -> {
                LlmEvent payload = (LlmEvent) event.getPayload();
                String reasoningContent = payload.response().getChoices().get(0).getMessage().getReasoningContent();
                log.info("[【{}】,sessionId={},parentTaskId={},taskId={},agentId={}]\n{}", event.getPayload().type().getName(),
                        event.getSessionId(), event.getParentTaskId(), event.getTaskId(), event.getAgentId(), reasoningContent);
            }
            case ERROR_EVENT -> {
                ErrorEvent payload = (ErrorEvent) event.getPayload();
                log.error("[【{}】,sessionId={},parentTaskId={},taskId={},agentId={}]", event.getPayload().type().getName(),
                        event.getSessionId(), event.getParentTaskId(), event.getTaskId(), event.getAgentId(), payload.ex());
            }
            default -> {
                try {
                    log.info("[【{}】,sessionId={},parentTaskId={},taskId={},agentId={}]\n{}", event.getPayload().type().getName(),
                            event.getSessionId(), event.getParentTaskId(), event.getTaskId(), event.getAgentId(), ObjectMapperSingleton.getObjectMapper().writeValueAsString(event.getPayload()));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
