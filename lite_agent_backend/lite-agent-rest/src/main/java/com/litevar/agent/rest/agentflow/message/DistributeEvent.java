package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

import java.util.List;

/**
 * agent分发消息
 *
 * @author uncle
 * @since 2025/12/18 15:54
 */
public record DistributeEvent(String taskId, String cmd, List<String> imageUrl, String videoUrl, String targetAgentId,
                            String dispatchId) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.AGENT_DISPATCH_EVENT;
    }
}
