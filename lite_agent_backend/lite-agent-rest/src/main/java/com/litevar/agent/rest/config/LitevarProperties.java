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
    @Value("${litevar.public.ip}")
    private String publicIp;
    @Value("${litevar.public.port}")
    private Integer publicPort;
    @Value("${litevar.temp.path}")
    private String tempPath;

}
