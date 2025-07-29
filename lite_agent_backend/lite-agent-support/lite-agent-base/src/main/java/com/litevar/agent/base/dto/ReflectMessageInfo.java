package com.litevar.agent.base.dto;

import lombok.Data;

/**
 * @author uncle
 * @since 2025/3/10 10:44
 */
@Data
public class ReflectMessageInfo {
    /**
     * 用户输入的消息
     */
    private String input;
    /**
     * 最高分
     */
    private Double score = -1d;
    /**
     * 最高分反思agent对应的输出
     */
    private String output;
    /**
     * 反思次数
     */
    private Integer reflectCount = 0;
}
