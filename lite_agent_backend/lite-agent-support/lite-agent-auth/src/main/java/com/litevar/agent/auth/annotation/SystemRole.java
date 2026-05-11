package com.litevar.agent.auth.annotation;

import com.litevar.agent.base.enums.SystemRoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统角色权限
 *
 * @author uncle
 * @since 2025/12/25 19:21
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SystemRole {

    SystemRoleEnum[] value() default {};
}
