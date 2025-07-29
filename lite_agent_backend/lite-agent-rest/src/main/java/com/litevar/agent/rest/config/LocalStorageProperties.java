package com.litevar.agent.rest.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author reid
 * @since 2/28/25
 */

@Data
@Component
public class LocalStorageProperties {
    @Value("${file.icon.path}")
    private String iconPath;

    @Value("${file.dataset.path}")
    private String datasetFilePath;
}
