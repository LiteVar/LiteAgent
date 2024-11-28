package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账号状态枚举
 *
 * @author reid
 * @since 2024/7/26
 */
@Getter
@AllArgsConstructor
public enum AccountStatus {
    ACTIVE(0),
    ;

    private final Integer value;
}
