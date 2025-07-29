package com.litevar.agent.rest.openai.message;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author uncle
 * @since 2025/4/16 16:16
 */
@Data
@AllArgsConstructor
public class TaskDoneMessage implements AgentMessage {
    private String sessionId;
    private String agentId;
    private String taskId;
}
