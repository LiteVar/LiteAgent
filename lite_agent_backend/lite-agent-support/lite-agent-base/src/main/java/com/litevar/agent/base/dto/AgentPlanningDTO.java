package com.litevar.agent.base.dto;

import lombok.Data;

import java.util.List;

/**
 * 规划agent信息
 *
 * @author uncle
 * @since 2025/6/3 15:28
 */
@Data
public class AgentPlanningDTO {
    /**
     * 规则内容(agent名字)
     */
    private String name;
    /**
     * 规划使用的模型
     */
    private PlanModel model;
    /**
     * 规则调用的工具
     */
    private List<PlanTool> tools;

    private PlanDescription description;

    /**
     * 子agent
     */
    private List<AgentPlanningDTO> children;

    @Data
    public static class PlanModel {
        /**
         * 模型id
         */
        private String id;
        /**
         * 模型名称
         */
        private String name;
    }

    @Data
    public static class PlanTool {
        /**
         * tool id
         */
        private String id;
        /**
         * tool name
         */
        private String name;
        /**
         * tool desc
         */
        private String desc;
    }

    @Data
    public static class PlanDescription {
        /**
         * 职责
         */
        private String duty;
        /**
         * 约束
         */
        private String constraint;
    }
}
