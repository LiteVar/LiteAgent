package com.litevar.agent.rest.openai.message;

import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * @author uncle
 * @since 2025/4/16 16:16
 */
@Getter
public class TaskDoneMessage implements AgentMessage {
    private final String sessionId;
    private final String agentId;
    private final String taskId;
    private final String requestId;

    public TaskDoneMessage() {
        this.sessionId = CurrentAgentRequest.getSessionId();
        this.agentId = CurrentAgentRequest.getAgentId();
        this.taskId = CurrentAgentRequest.getTaskId();
        this.requestId = CurrentAgentRequest.getRequestId();
    }
}
