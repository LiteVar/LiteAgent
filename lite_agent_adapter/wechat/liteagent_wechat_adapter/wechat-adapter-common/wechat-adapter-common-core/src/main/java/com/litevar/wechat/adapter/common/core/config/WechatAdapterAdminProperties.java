package com.litevar.wechat.adapter.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Teoan
 * @since 2025/8/26 16:06
 */
@Configuration
@ConfigurationProperties(prefix = "wechat-adapter.admin")
@Data
public class WechatAdapterAdminProperties {

    private String username;

    private String password;

}
