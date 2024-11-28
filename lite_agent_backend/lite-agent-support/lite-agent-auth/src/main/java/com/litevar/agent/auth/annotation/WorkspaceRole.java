package com.litevar.agent.auth.annotation;

import com.litevar.agent.base.enums.RoleEnum;

import java.lang.annotation.*;

/**
 * 工作空间操作需要的角色权限
 *
 * @author uncle
 * @since 2024/8/5 09:46
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface WorkspaceRole {

    RoleEnum[] value() default {};
}
