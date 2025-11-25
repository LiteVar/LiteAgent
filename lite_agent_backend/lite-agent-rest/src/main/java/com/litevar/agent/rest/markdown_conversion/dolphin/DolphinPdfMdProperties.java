package com.litevar.agent.rest.markdown_conversion.dolphin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dolphin")
public class DolphinPdfMdProperties {

    /**
     * Service base URL, e.g. https://bm-api.prmeasure.com:8082
     */
    private String baseUrl;

    /**
     * Bearer token used for authorization. Should be provided via external configuration.
     */
    private String token;

    /**
     * Interval between task-status polling requests.
     */
    private Duration pollInterval = Duration.ofSeconds(3);

    /**
     * Maximum time to wait for the remote conversion to finish.
     */
    private Duration pollTimeout = Duration.ofMinutes(5);

    /**
     * Timeout applied when downloading the converted archive.
     */
    private Duration downloadTimeout = Duration.ofMinutes(2);

    /**
     * Timeout applied to upload/status requests when blocking.
     */
    private Duration requestTimeout = Duration.ofSeconds(60);
}
