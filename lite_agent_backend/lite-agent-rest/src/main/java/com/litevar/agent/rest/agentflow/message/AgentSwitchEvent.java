package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

/**
 * agent切换消息
 *
 * @author uncle
 * @since 2025/12/18 15:45
 */
public record AgentSwitchEvent(String taskId, String agentId, String agentName) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.AGENT_SWITCH_EVENT;
    }
}
