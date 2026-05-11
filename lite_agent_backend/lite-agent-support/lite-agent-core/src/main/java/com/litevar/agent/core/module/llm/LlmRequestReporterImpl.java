package com.litevar.agent.core.module.llm;

import com.litevar.agent.base.llm.LlmRequestReporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author uncle
 * @since 2026/1/21 09:57
 */
@Component
public class LlmRequestReporterImpl implements LlmRequestReporter {
    @Autowired
    private TokenUsageService tokenUsageService;

    @Override
    public void checkBalance(String userId, String modelId) {
    }

    @Override
    public void report(String userId, String modelId, String agentId, Integer promptTokens, Integer completionTokens) {
        tokenUsageService.addUsage(userId, modelId, agentId, promptTokens, completionTokens);
    }
}
