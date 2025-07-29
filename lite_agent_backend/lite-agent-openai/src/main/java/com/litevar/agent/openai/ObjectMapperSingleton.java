package com.litevar.agent.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * @author uncle
 * @since 2025/2/13 17:46
 */
public enum ObjectMapperSingleton {
    INSTANCE;
    private final ObjectMapper objectMapper;

    ObjectMapperSingleton() {
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS)
        );
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ObjectMapper getObjectMapper() {
        return INSTANCE.objectMapper;
    }
}
