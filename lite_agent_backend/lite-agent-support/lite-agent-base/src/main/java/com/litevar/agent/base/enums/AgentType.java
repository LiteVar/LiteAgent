package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * agent 类型
 *
 * @author uncle
 * @since 2025/3/10 17:20
 */
@Getter
@AllArgsConstructor
public enum AgentType {
    /**
     * 普通
     */
    GENERAL(0),
    /**
     * 分发
     */
    DISTRIBUTE(1),
    /**
     * 反思
     */
    REFLECTION(2);

    public final Integer type;

    public static AgentType of(Integer type) {
        for (AgentType value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
