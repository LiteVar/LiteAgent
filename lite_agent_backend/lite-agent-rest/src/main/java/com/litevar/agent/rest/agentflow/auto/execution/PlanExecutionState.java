package com.litevar.agent.rest.agentflow.auto.execution;

import com.litevar.agent.rest.agentflow.bean.PlanAgentState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 计划结果的执行态
 *
 * @author uncle
 * @since 2026/03/18 18:29
 */
@Data
public class PlanExecutionState {
    /**
     * 规划ID
     */
    private String planId;
    /**
     * agent状态列表
     */
    private List<PlanAgentState> agents = new ArrayList<>();
}
