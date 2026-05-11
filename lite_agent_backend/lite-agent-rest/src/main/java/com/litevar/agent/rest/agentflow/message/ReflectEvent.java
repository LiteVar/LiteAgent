package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.base.response.ReflectResult;
import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

import java.util.List;

/**
 * 反思消息
 *
 * @author uncle
 * @since 2025/12/18 15:55
 */
public record ReflectEvent(String rawInput, String rawOutput,
                         List<ReflectResult> reflectOutput) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.REFLECTION_EVENT;
    }
}
