package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.dto.AgentPlanningDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 规划内容消息
 *
 * @author uncle
 * @since 2025/6/12 16:21
 */
@Data
@AllArgsConstructor
public class PlanningMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    private List<AgentPlanningDTO> taskList;
    private String planId;
}
