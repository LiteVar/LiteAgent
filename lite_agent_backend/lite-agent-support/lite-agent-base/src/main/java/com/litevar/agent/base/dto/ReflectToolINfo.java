package com.litevar.agent.base.dto;

import lombok.Data;

import java.util.List;

/**
 * 工具序列反思
 *
 * @author uncle
 * @since 2025/3/20 17:56
 */
@Data
public class ReflectToolINfo {
    private String taskId;
    /**
     * 接下来应执行的function
     */
    private List<String> sequence;

    /**
     * 反思失败次数
     */
    private Integer count = 0;
}