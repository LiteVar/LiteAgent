package com.litevar.agent.rest.openai.message;

import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * agent切换消息
 *
 * @author uncle
 * @since 2025/4/16 12:20
 */
@Getter
public class AgentSwitchMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String requestId;
    private final String parentTaskId;

    private final String agentId;
    private final String agentName;

    public AgentSwitchMessage(String agentId, String agentName, String taskId) {
        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = taskId;
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getTaskId();

        this.agentId = agentId;
        this.agentName = agentName;
    }
}
