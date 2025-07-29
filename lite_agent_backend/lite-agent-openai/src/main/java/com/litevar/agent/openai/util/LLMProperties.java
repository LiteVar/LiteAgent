package com.litevar.agent.openai.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author uncle
 * @since 2025/5/16 11:23
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {
    private Integer timeout;
}
