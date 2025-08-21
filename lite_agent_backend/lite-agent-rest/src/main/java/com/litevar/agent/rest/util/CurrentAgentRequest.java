package com.litevar.agent.rest.util;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;

/**
 * 用于存储当前agent请求信息
 *
 * @author uncle
 * @since 2025/7/17
 */
public class CurrentAgentRequest {

    private static final ThreadLocal<AgentRequest> CONTEXT = new ThreadLocal<>();

    public static void setContext(AgentRequest context) {
        CONTEXT.set(context);
    }

    public static AgentRequest getContext() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程的ThreadLocal变量，防止内存泄漏
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前上下文的sessionId
     */
    public static String getSessionId() {
        AgentRequest context = getContext();
        return context != null ? context.getSessionId() : null;
    }

    /**
     * 获取当前上下文的taskId
     */
    public static String getTaskId() {
        AgentRequest context = getContext();
        return context != null ? context.getTaskId() : null;
    }

    /**
     * 获取当前上下文的requestId
     */
    public static String getRequestId() {
        AgentRequest context = getContext();
        return context != null ? context.getRequestId() : null;
    }

    /**
     * 获取当前上下文的agentId
     */
    public static String getAgentId() {
        AgentRequest context = getContext();
        return context != null ? context.getAgentId() : null;
    }

    /**
     * 捕获当前线程的上下文，用于异步操作
     */
    public static AgentRequest capture() {
        AgentRequest current = getContext();
        if (current == null) {
            return null;
        }
        // 创建一个副本，避免引用问题
        return BeanUtil.copyProperties(current, AgentRequest.class);
    }

    /**
     * 在异步操作中恢复上下文
     */
    public static void restore(AgentRequest context) {
        if (context != null) {
            setContext(context);
        }
    }

    /**
     * 包装Runnable以传递上下文到异步线程
     */
    public static Runnable wrapRunnable(Runnable runnable) {
        AgentRequest context = capture();
        return () -> {
            try {
                restore(context);
                runnable.run();
            } finally {
                clear();
            }
        };
    }

    /**
     * Agent上下文信息
     */
    @Data
    public static class AgentRequest {
        private String sessionId;
        private String parentTaskId;
        private String taskId;
        private String requestId;
        private String agentId;
    }
}