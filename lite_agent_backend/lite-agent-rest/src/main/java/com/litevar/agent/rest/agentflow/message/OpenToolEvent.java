package com.litevar.agent.rest.agentflow.message;

import cn.hutool.json.JSONObject;
import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

/**
 * open-tool 告诉第三方调用工具消息
 *
 * @author uncle
 * @since 2025/12/18 15:58
 */
public record OpenToolEvent(String callId, String name, JSONObject arguments) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.OPEN_TOOL_CALL_EVENT;
    }
}
