package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

/**
 * 工具调用结果消息
 *
 * @author uncle
 * @since 2025/12/18 15:36
 */
public record ToolResultEvent(String callId, String result, String functionId) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.TOOL_RESULT_EVENT;
    }
}
