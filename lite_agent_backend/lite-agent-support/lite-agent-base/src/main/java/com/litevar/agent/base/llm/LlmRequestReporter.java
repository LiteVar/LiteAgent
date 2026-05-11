package com.litevar.agent.base.llm;

/**
 * 大模型请求处理
 *
 * @author uncle
 * @since 2026/1/19 16:05
 */
public interface LlmRequestReporter {
    /**
     * 检查余额
     */
    void checkBalance(String userId, String modelId);

    /**
     * 上报token使用情况
     */
    void report(String userId, String modelId, String agentId, Integer promptTokens, Integer completionTokens);
}
