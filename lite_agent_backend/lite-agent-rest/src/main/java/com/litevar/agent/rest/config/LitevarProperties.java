package com.litevar.agent.rest.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author reid
 * @since 3/20/25
 */

@Data
@Component
public class LitevarProperties {
    @Value("${litevar.upload.path}")
    private String uploadPath;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Value("${file.save-path}")
    private String iconPath;
}
