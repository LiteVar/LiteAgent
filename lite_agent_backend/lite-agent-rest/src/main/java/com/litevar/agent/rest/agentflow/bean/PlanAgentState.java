package com.litevar.agent.rest.agentflow.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 计划agent状态
 *
 * @author uncle
 * @since 2026/03/18 18:29
 */
@Data
public class PlanAgentState {
    /**
     * 运行时agent ID
     */
    private String agentId;
    /**
     * agent名称
     */
    private String agentName;
    /**
     * 依赖agent ID
     */
    private List<String> dependencyAgentIds = new ArrayList<>();
    /**
     * agent状态
     */
    private PlanAgentStatus status;
    /**
     * agent职责
     */
    private String duty;
    /**
     * agent约束
     */
    private String constraint;
}
