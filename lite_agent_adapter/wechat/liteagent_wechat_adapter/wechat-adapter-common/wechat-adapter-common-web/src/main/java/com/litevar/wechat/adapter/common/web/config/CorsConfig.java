package com.litevar.wechat.adapter.common.web.config;

import cn.hutool.core.collection.CollUtil;
import com.litevar.wechat.adapter.common.core.config.WechatAdapterCorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 *全局CORS配置
 * @author Teoan
 * @since 2025/9/2 09:31
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final WechatAdapterCorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = corsProperties.getAllowedOrigins();
        if (CollUtil.isNotEmpty(allowedOrigins)) {
            registry.addMapping("/**") // 匹配所有路径，可以更具体如 "/api/**"
                    .allowedOrigins(allowedOrigins.toArray(new String[0])) // 从配置文件读取允许的来源
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                    // 或者更简洁地：.allowedMethods("*")
                    .allowedHeaders("*") // 允许的请求头，例如：Authorization, Content-Type, X-Requested-With
                    .allowCredentials(true) // 是否允许发送 Cookie、HTTP 认证信息等凭证
                    .maxAge(3600); // 预检请求（OPTIONS）的缓存时间，单位秒
        }
    }

}