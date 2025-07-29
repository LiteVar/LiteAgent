package com.litevar.agent.core.module.agent;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.AgentApiKey;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author uncle
 * @since 2025/3/21 18:28
 */
@Service
public class AgentApiKeyService extends ServiceImpl<AgentApiKey> {

    @Cacheable(value = CacheKey.AGENT_API_KEY, key = "#apiKey")
    public String agentIdFromApiKey(String apiKey) {
        AgentApiKey one = this.one(this.lambdaQuery()
                .projectDisplay(AgentApiKey::getAgentId)
                .eq(AgentApiKey::getApiKey, apiKey));
        if (one == null) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION);
        }
        return one.getAgentId();
    }
}
