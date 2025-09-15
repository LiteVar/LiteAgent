package com.litevar.dingtalk.adapter.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Teoan
 * @since 2025/8/25 10:12
 */
@Configuration
@ConfigurationProperties(prefix = "dingtalk-adapter.project")
@Data
public class DingtalkAdapterProjectProperties {

    private String version;
}
