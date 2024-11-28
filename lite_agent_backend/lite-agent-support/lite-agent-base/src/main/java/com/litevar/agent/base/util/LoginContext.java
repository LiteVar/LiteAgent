package com.litevar.agent.base.util;

import com.litevar.agent.base.vo.LoginUser;

/**
 * 当前登录用户上下文
 *
 * @author uncle
 * @since 2024/8/2 17:04
 */
public class LoginContext {
    private LoginContext() {
    }

    private static final ThreadLocal<LoginUser> THREAD_LOCAL = new ThreadLocal<>();

    public static LoginUser me() {
        return THREAD_LOCAL.get();
    }

    public static void set(LoginUser loginUser) {
        THREAD_LOCAL.set(loginUser);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static String currentUserId() {
        return me().getId();
    }
}
