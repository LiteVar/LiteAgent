package com.litevar.agent.base.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * System role enum
 *
 * @author uncle
 * @since 2025/12/25 19:21
 */
@Getter
@AllArgsConstructor
public enum SystemRoleEnum {
    /**
     * 普通用户
     */
    ROLE_USER(0, "user"),
    /**
     * 系统管理员
     */
    ROLE_SYSTEM_ADMIN(1, "system_admin"),
    ;

    @JsonValue
    private final Integer systemRole;
    private final String name;

    public static SystemRoleEnum of(Integer code) {
        for (SystemRoleEnum roleEnum : values()) {
            if (roleEnum.systemRole.equals(code)) {
                return roleEnum;
            }
        }
        return null;
    }
}
