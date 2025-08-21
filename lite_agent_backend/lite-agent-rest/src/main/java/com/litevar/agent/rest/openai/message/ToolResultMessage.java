package com.litevar.agent.rest.openai.message;

import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * 工具调用结果消息
 *
 * @author uncle
 * @since 2025/4/11 16:50
 */
@Getter
public class ToolResultMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String agentId;
    private final String requestId;
    private final String parentTaskId;

    private final String callId;
    private final String result;

    private String functionId;

    public ToolResultMessage(String callId, String result, String functionId) {
        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = CurrentAgentRequest.getTaskId();
        this.agentId = CurrentAgentRequest.getAgentId();
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getContext().getParentTaskId();

        this.callId = callId;
        this.result = result;
        this.functionId = functionId;
    }
}
