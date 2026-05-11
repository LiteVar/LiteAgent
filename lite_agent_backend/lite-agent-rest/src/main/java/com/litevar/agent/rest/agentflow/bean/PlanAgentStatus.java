package com.litevar.agent.rest.agentflow.bean;

/**
 * 计划agent执行状态
 *
 * @author uncle
 * @since 2026/03/18 18:29
 */
public enum PlanAgentStatus {
    /**
     * 等待依赖完成
     */
    PENDING,
    /**
     * 依赖已满足,可执行
     */
    READY,
    /**
     * 执行中
     */
    RUNNING,
    /**
     * agent执行已结束
     */
    FINISHED,
    /**
     * agent结果已验收通过
     */
    ACCEPTED
}
