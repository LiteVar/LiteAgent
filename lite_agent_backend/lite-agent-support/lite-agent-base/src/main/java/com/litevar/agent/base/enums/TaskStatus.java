package com.litevar.agent.base.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举 - 记录任务执行的具体阶段
 *
 * @author uncle
 * @since 2025/8/11
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {
    /**
     * 等待中
     */
    PENDING(0, "等待处理消息"),
    /**
     * 调用大模型中
     */
    CALLING_LLM(1, "开始调用大模型"),
    /**
     * 执行函数调用中
     */
    CALLING_FUNCTION(2, "开始调用工具"),
    /**
     * 调用子agent中
     */
    CALLING_SUB_AGENT(3, "开始调用子agent"),
    /**
     * 知识库检索中
     */
    RETRIEVING_KNOWLEDGE(4, "开始检索知识库"),
    /**
     * 已完成
     */
    COMPLETED(5, "任务结束"),
    /**
     * 失败
     */
    FAILED(6, "任务失败");

    private final Integer status;
    private final String message;

    public static TaskStatus of(Integer status) {
        for (TaskStatus taskStatus : TaskStatus.values()) {
            if (ObjectUtil.equals(taskStatus.status, status)) {
                return taskStatus;
            }
        }
        return null;
    }
}