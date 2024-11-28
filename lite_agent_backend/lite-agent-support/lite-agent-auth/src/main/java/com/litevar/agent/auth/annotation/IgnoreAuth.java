package com.litevar.agent.auth.annotation;

import java.lang.annotation.*;

/**
 * 标记忽略校验的接口
 *
 * @author uncle
 * @since 2024/7/4 12:00
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IgnoreAuth {
}
