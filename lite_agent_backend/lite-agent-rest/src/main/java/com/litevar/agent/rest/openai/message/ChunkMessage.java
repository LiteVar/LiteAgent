package com.litevar.agent.rest.openai.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * stream流片段消息
 *
 * @author uncle
 * @since 2025/4/15 17:59
 */
@Getter
@AllArgsConstructor
public class ChunkMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;
    private Integer chunkType;

    private String part;
}
