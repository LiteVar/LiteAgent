package com.litevar.liteagent.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author reid
 * @since 2025/6/23
 */

public class LiteAgentClient {

    private final RestClient restClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public LiteAgentClient(String baseUrl, String apiKey) {
        if (baseUrl.isBlank()) {
            baseUrl = ApiEndpoints.BASE_URL;
        }

        Assert.hasText(apiKey, "apiKey must not be null");

        Consumer<HttpHeaders> headersConsumer = headers -> {
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
        };

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders(headersConsumer)
            .build();

        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders(headersConsumer)
            .build();

        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取Lite Agent版本
     *
     * @return 版本信息
     */
    public String getVersion() {
        return restClient.get().uri(ApiEndpoints.GET_VERSION).retrieve()
            .body(ApiRecords.VersionResponse.class)
            .version();
    }

    /**
     * 初始化会话
     *
     * @return 会话ID
     */
    public String initSession() {
        return restClient.post().uri(ApiEndpoints.INIT_SESSION).retrieve()
            .body(ApiRecords.SessionResponse.class)
            .sessionId();
    }

    /**
     * 清除会话
     *
     * @param sessionId 会话id
     * @return
     */
    public String clearSession(String sessionId) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path(ApiEndpoints.CLEAR).queryParam("sessionId", sessionId).build())
            .retrieve()
            .body(ApiRecords.ClearResponse.class)
            .id();
    }

    /**
     * 停止会话
     *
     * @param sessionId 会话id
     * @param taskId    任务id, 可选
     * @return
     */
    public ApiRecords.StopResponse stopSession(String sessionId, @Nullable String taskId) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path(ApiEndpoints.STOP)
                .queryParam("sessionId", sessionId)
                .queryParam("taskId", taskId)
                .build()
            )
            .retrieve()
            .body(ApiRecords.StopResponse.class);
    }

    /**
     * 返回function call执行结果给lite agent
     *
     * @param sessionId
     * @param request
     * @return
     */
    public String callback(String sessionId, ApiRecords.CallbackRequest request) {
        return restClient.post()
            .uri(uriBuilder -> uriBuilder.path(ApiEndpoints.CALLBACK).queryParam("sessionId", sessionId).build())
            .body(request)
            .retrieve()
            .toEntity(String.class)
            .getBody();
    }

    /**
     * 获取聊天历史记录
     *
     * @param sessionId
     * @return
     */
    public List<ApiRecords.AgentMessage> chatHistory(String sessionId) {
        ParameterizedTypeReference<List<ApiRecords.AgentMessage>> type = new ParameterizedTypeReference<>() {};

        return restClient.get()
            .uri(uriBuilder -> uriBuilder.path(ApiEndpoints.HISTORY).queryParam("sessionId", sessionId).build())
            .retrieve()
            .body(type);
    }

    public Flux<ServerSentEvent<ApiRecords.AgentMessage>> chat(String sessionId, ApiRecords.ChatRequest request, MessageHandler handler) {
        // 定义我们期望接收的泛型类型
        ParameterizedTypeReference<ServerSentEvent<ApiRecords.AgentMessage>> type = new ParameterizedTypeReference<>() {};

        return webClient.post()
            .uri(uriBuilder -> uriBuilder.path(ApiEndpoints.CHAT).queryParam("sessionId", sessionId).build())
            .bodyValue(request)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(type)
            .doOnNext(event -> {
                String eventType = event.event();
                ApiRecords.AgentMessage data = event.data();

                switch (eventType) {
                    case "message" -> handler.handleMessage(data);
                    case "chunk" -> handler.handleChunk(data);
                    case "functionCall" -> handler.handleFunctionCall(data);
                }
            });
    }

}
