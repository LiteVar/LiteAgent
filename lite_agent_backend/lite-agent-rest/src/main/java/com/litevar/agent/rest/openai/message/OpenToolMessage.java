package com.litevar.agent.rest.openai.message;

import cn.hutool.json.JSONObject;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * @author uncle
 * @since 2025/5/7 16:36
 */
@Getter
public class OpenToolMessage implements AgentMessage {
    private final String agentId;
    private final String sessionId;
    private final String taskId;
    private final String requestId;
    private final String parentTaskId;

    private final String callId;
    private final String name;
    private final JSONObject arguments;

    public OpenToolMessage(CurrentAgentRequest.AgentRequest context, String callId, String name, JSONObject arguments) {
        this.agentId = context.getAgentId();
        this.sessionId = context.getSessionId();
        this.taskId = context.getTaskId();
        this.requestId = context.getRequestId();
        this.parentTaskId = context.getParentTaskId();
        this.callId = callId;
        this.name = name;
        this.arguments = arguments;
    }
}
