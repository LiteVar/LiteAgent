package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * agent调用方式
 *
 * @author uncle
 * @since 2025/4/8 15:21
 */
@Getter
@AllArgsConstructor
public enum AgentCallType {
    /**
     * 本系统agent直接调用
     */
    AGENT(0),
    /**
     * 开放api外部调用
     */
    EXTERNAL(1);

    private final Integer callType;
}
