package com.litevar.agent.rest.config;

import cn.hutool.core.util.StrUtil;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Qdrant客户端配置
 *
 * @author uncle
 * @since 2026/04/28 10:40
 */
@Configuration
public class QdrantClientConfig {
    @Value("${qdrant.host}")
    private String host;
    @Value("${qdrant.port:6334}")
    private int port;
    @Value("${qdrant.use-tls:false}")
    private boolean useTls;
    @Value("${qdrant.api-key:}")
    private String apiKey;
    @Value("${qdrant.timeout-seconds:10}")
    private long timeoutSeconds;

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, useTls)
                .withTimeout(Duration.ofSeconds(timeoutSeconds));
        if (StrUtil.isNotBlank(apiKey)) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }
}
