package com.litevar.agent.base.util;

/**
 * 当前线程调用LLM用户上下文
 * 部分开放接口无法通过登录用户获取用户ID，需要通过此上下文传递用户ID
 *
 * @author reid
 * @since 2025/12/26
 */

public class LlmContext {
    private static final ThreadLocal<Context> THREAD_LOCAL = new ThreadLocal<>();

    private static Context get() {
        return THREAD_LOCAL.get();
    }

    public static String getUserId() {
        Context context = get();
        return context == null ? "" : context.userId;
    }

    public static String getAgentId() {
        Context context = get();
        return context == null ? "" : context.agentId;
    }

    public static void set(Context context) {
        THREAD_LOCAL.set(context);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public record Context(String userId, String agentId) {
    }
}
