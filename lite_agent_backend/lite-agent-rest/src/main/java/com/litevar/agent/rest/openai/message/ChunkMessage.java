package com.litevar.agent.rest.openai.message;

import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * stream流片段消息
 *
 * @author uncle
 * @since 2025/4/15 17:59
 */
@Getter
public class ChunkMessage implements AgentMessage {
    private final String sessionId;
    private final String parentTaskId;
    private final String taskId;
    private final String agentId;
    private final String requestId;
    private final Integer chunkType;

    private final String part;

    public ChunkMessage(CurrentAgentRequest.AgentRequest context, Integer chunkType, String part) {
        this.chunkType = chunkType;
        this.part = part;
        this.sessionId = context.getSessionId();
        this.taskId = context.getTaskId();
        this.agentId = context.getAgentId();
        this.requestId = context.getRequestId();
        this.parentTaskId = context.getParentTaskId();
    }

    public ChunkMessage(Integer chunkType, String part) {
        this(CurrentAgentRequest.getContext(), chunkType, part);
    }
}
