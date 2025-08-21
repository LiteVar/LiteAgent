package com.litevar.agent.rest.openai.message;

import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * 异常消息
 *
 * @author uncle
 * @since 2025/4/15 17:48
 */
@Getter
public class ErrorMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String agentId;
    private final String requestId;
    private final String parentTaskId;

    private final Throwable ex;

    public ErrorMessage(CurrentAgentRequest.AgentRequest context, Throwable ex) {
        this.sessionId = context.getSessionId();
        this.taskId = context.getTaskId();
        this.agentId = context.getAgentId();
        this.requestId = context.getRequestId();
        this.parentTaskId = context.getParentTaskId();

        this.ex = ex;
    }
}
