package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

/**
 * @author uncle
 * @since 2025/12/18 17:21
 */
public record ChunkEvent (Integer chunkType, String part) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.CHUNK_EVENT;
    }
}
