package com.litevar.agent.core.util;

import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author reid
 * @since 2025/5/15
 */

@Slf4j
public class McpUtil {
//    private static final String MCP_CLIENT_NAME = "spring-ai-mcp-client";
    private static final String MCP_CLIENT_VERSION = "1.0.0";

    private static final Map<String, McpSyncClient> SYNC_CLIENTS = new HashMap<>();
    private static final Map<String, McpAsyncClient> ASYNC_CLIENTS = new HashMap<>();

    public static McpSyncClient getSyncClient(String name, String baseUrl, String sseEndpoint) {
        // 检查是否已经创建了客户端
//        String clientKey = MD5.create().digestHex(baseUrl + sseEndpoint);
//        if (SYNC_CLIENTS.containsKey(clientKey)) {
//            return SYNC_CLIENTS.get(clientKey);
//        }

        // 创建新的客户端并存储
        McpSyncClient client = createSyncClient(name, baseUrl, sseEndpoint);
//        SYNC_CLIENTS.put(clientKey, client);
        return client;
    }

    public static McpAsyncClient getAsyncClient(String name, String baseUrl, String sseEndpoint) {
        // 检查是否已经创建了客户端
//        String clientKey = MD5.create().digestHex(baseUrl + sseEndpoint);
//        if (ASYNC_CLIENTS.containsKey(clientKey)) {
//            return ASYNC_CLIENTS.get(clientKey);
//        }

        // 创建新的客户端并存储
        McpAsyncClient client = createAsyncClient(name, baseUrl, sseEndpoint);
//        ASYNC_CLIENTS.put(clientKey, client);
        return client;
    }

    public static McpSyncClient createSyncClient(String name, String baseUrl, String sseEndpoint) {
        // 创建并返回一个 McpSyncClient 实例
        McpSyncClient client = McpClient.sync(createTransport(baseUrl, sseEndpoint))
                .clientInfo(new McpSchema.Implementation(name, MCP_CLIENT_VERSION))
                .requestTimeout(Duration.ofMinutes(2))
                .build();

        // 初始化客户端
        McpSchema.InitializeResult initializeResult = client.initialize();
        log.info("McpSyncClient initialize result: {}", initializeResult);

        return client;
    }

    public static McpAsyncClient createAsyncClient(String name, String baseUrl, String sseEndpoint) {
        // 根据你的配置创建并返回一个 McpAsyncClient 实例
        McpAsyncClient client = McpClient.async(createTransport(baseUrl, sseEndpoint))
                .clientInfo(new McpSchema.Implementation(name, MCP_CLIENT_VERSION))
                .requestTimeout(Duration.ofMinutes(2))
                .build();

        // 初始化客户端
        Mono<McpSchema.InitializeResult> mono = client.initialize();
        mono.subscribe(initializeResult -> {
            log.info("McpAsyncClient initialize result: {}", initializeResult);
        }, throwable -> {
            log.error("McpAsyncClient initialization failed", throwable);
        });

        return client;
    }

    static McpClientTransport createTransport(String baseUrl, String sseEndpoint) {
        // 创建并返回一个 McpClientTransport 实例
        return HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint(sseEndpoint)
                .objectMapper(new ObjectMapper())
                .build();
    }
}
