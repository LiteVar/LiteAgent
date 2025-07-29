package com.litevar.agent.base.response;

import lombok.Data;

/**
 * 反思结果
 *
 * @author uncle
 * @since 2025/3/4 12:08
 */
@Data
public class ReflectResult {
    /**
     * 分数
     */
    private Double score;
    /**
     * 结论
     */
    private String information;
}
