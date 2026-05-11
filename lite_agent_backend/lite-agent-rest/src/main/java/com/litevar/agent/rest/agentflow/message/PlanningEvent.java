package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

import java.util.List;

/**
 * 规划内容消息
 *
 * @author uncle
 * @since 2025/12/18 15:43
 */
public record PlanningEvent(List<AgentPlanningDTO> taskList, String planId) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.PLANNING_EVENT;
    }
}
