package com.litevar.agent.rest.agentflow.event;

/**
 * 事件消息payload
 *
 * @author uncle
 * @since 2025/12/18 14:53
 */
public interface AgentEventPayload {
    /**
     * 事件类型
     */
    AgentEventType type();
}
