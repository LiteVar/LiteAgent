package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

import java.util.List;

/**
 * 规划内容消息
 *
 * @author uncle
 * @since 2025/6/12 16:21
 */
@Getter
public class PlanningMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String agentId;
    private final String requestId;
    private final String parentTaskId;

    private final List<AgentPlanningDTO> taskList;
    private final String planId;

    public PlanningMessage(String planId, List<AgentPlanningDTO> taskList) {
        this.planId = planId;
        this.taskList = taskList;

        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = CurrentAgentRequest.getTaskId();
        this.agentId = CurrentAgentRequest.getAgentId();
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getContext().getParentTaskId();
    }
}
