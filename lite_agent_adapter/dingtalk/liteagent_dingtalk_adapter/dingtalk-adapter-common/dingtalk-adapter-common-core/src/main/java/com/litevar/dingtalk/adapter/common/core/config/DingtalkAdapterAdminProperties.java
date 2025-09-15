package com.litevar.dingtalk.adapter.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Teoan
 * @since 2025/8/26 16:06
 */
@Configuration
@ConfigurationProperties(prefix = "dingtalk-adapter.admin")
@Data
public class DingtalkAdapterAdminProperties {

    private String username;

    private String password;

}
