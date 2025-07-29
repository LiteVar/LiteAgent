package com.litevar.agent.core.module.tool;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.ToolFunction;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author uncle
 * @since 2024/11/26 14:29
 */
@Service
public class ToolFunctionService extends ServiceImpl<ToolFunction> {

    @Cacheable(value = CacheKey.TOOL_FUNCTION_INFO, key = "#id", unless = "#result == null")
    public ToolFunction findById(String id) {
        return Optional.ofNullable(this.getById(id)).orElseThrow();
    }
}
