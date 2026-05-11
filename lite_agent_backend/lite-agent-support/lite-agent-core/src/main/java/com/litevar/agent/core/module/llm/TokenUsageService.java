package com.litevar.agent.core.module.llm;

import com.litevar.agent.base.entity.TokenUsageRecord;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author reid
 * @since 2025/12/19
 */

@Slf4j
@Service
public class TokenUsageService extends ServiceImpl<TokenUsageRecord> {

    public void addUsage(
            String userId, String modelId, String agentId, Integer promptTokens, Integer completionTokens
    ) {
        log.info("用户[{}],agentId[{}],使用模型[{}]消耗token[{}]", userId, agentId, modelId, promptTokens + completionTokens);
    }

    public void checkEnoughPoints(String userId, String modelId) {
    }
}
