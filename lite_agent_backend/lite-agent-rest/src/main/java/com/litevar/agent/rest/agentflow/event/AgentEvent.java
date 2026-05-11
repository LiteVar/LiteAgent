package com.litevar.agent.rest.agentflow.event;

import com.litevar.agent.rest.agentflow.bean.AgentContext;
import lombok.Getter;

/**
 * agent事件消息
 *
 * @author uncle
 * @since 2025/12/18 14:55
 */
@Getter
public class AgentEvent {
    private final String sessionId;
    private final String requestId;
    private final String parentTaskId;
    private final String taskId;
    private final String agentId;

    private final AgentEventPayload payload;

    public AgentEvent(AgentContext context, AgentEventPayload payload) {
        this.sessionId = context.getSessionId();
        this.requestId = context.getRequestId();
        this.parentTaskId = context.getParentTaskId();
        this.taskId = context.getTaskId();
        this.agentId = context.getAgentId();
        this.payload = payload;
    }
}
