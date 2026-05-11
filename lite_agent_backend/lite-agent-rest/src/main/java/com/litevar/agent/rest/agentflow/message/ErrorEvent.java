package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

/**
 * 异常消息
 *
 * @author uncle
 * @since 2025/12/18 18:04
 */
public record ErrorEvent(Throwable ex) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.ERROR_EVENT;
    }
}
