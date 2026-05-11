package com.litevar.agent.rest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author reid
 * @since 2026/1/23
 */

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String type;
    private String basePathLocal;
    private String basePathOss;
    private String imagePath;
    private String audioPath;
    private String videoPath;
    private String documentPath;
    // 文件访问超时时间，单位秒，默认1800秒(30分钟)
    private Long accessTimeout;

    public String getBasePath() {
        return getType().equalsIgnoreCase("local") ? getBasePathLocal() : getBasePathOss();
    }
}
