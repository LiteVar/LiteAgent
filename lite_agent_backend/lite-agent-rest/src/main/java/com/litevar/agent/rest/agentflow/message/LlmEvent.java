package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

/**
 * 大模型响应内容的消息
 *
 * @author uncle
 * @since 2025/12/18 17:38
 */
public record LlmEvent(Integer agentType, CompletionResponse response, AgentEventType type) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return type;
    }
}
