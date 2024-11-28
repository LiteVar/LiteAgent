package com.litevar.agent.base.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色
 *
 * @author uncle
 * @since 2024/7/3 16:46
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {
    /**
     * 普通用户
     */
    ROLE_USER(0, "user"),
    /**
     * 开发者
     */
    ROLE_DEVELOPER(2, "developer"),
    /**
     * 空间管理员
     */
    ROLE_ADMIN(3, "admin"),
    ;

    @JsonValue
    private final Integer code;
    private final String name;

    public static RoleEnum of(Integer code) {
        for (RoleEnum roleEnum : values()) {
            if (roleEnum.code.equals(code)) {
                return roleEnum;
            }
        }
        return null;
    }
}
