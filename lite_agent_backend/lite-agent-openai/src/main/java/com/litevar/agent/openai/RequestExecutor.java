package com.litevar.agent.openai;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.openai.completion.CompletionRequestParam;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.util.LLMProperties;
import com.litevar.agent.openai.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author uncle
 * @since 2025/2/12 17:20
 */
@Slf4j
public class RequestExecutor {
    private static Integer timeout = 300;
    //重试次数
    private static final int maxAttempts = 3;
    private static final String chatCompletion = "/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final String BEARER_PREFIX = "Bearer ";
    private static OkHttpClient okHttpClient;

    private RequestExecutor() {
    }

    public static CompletionResponse doRequest(CompletionRequestParam param, String url, String key) {
        Map<String, String> headers = new HashMap<>(2);
        if (StrUtil.isNotEmpty(key)) {
            headers.put(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + key);
        }
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
        String data;
        try {
            data = ObjectMapperSingleton.getObjectMapper().writeValueAsString(param);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Request request = new Request.Builder()
                .url(url + chatCompletion)
                .post(RequestBody.create(data, JSON_MEDIA_TYPE))
                .headers(Headers.of(headers))
                .build();
        return RetryUtil.withRetry(() -> {
            try (Response response = getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    try (ResponseBody errorBody = response.body()) {
                        String errorContent = errorBody != null ? errorBody.string() : "空响应体";
                        throw new RuntimeException("request failed:" + response.code() + "," + errorContent);
                    }
                }
                try (ResponseBody successBody = response.body()) {
                    String res = successBody.string();
                    return ObjectMapperSingleton.getObjectMapper().readValue(res, CompletionResponse.class);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, maxAttempts);
    }

    public static void doStreamRequest(CompletionRequestParam param, String url, String key, RequestCallback callback) {
        String data;
        try {
            data = ObjectMapperSingleton.getObjectMapper().writeValueAsString(param);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> headers = new HashMap<>(3);
        if (StrUtil.isNotEmpty(key)) {
            headers.put(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + key);
        }
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.put(HttpHeaders.ACCEPT, "text/event-stream");

        doStreamRequestInternal(data, url, headers, callback, 1);
    }

    private static void doStreamRequestInternal(String data, String url, Map<String, String> headers, RequestCallback callback, int attempt) {
        Request request = new Request.Builder()
                .url(url + chatCompletion)
                .post(RequestBody.create(data, JSON_MEDIA_TYPE))
                .headers(Headers.of(headers))
                .build();
        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (attempt <= maxAttempts) {
                    log.warn("Exception was thrown on attempt %s of %s".formatted(attempt, maxAttempts), e);
                    RetryUtil.sleep(attempt);
                    doStreamRequestInternal(data, url, headers, callback, attempt + 1);
                } else {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = body != null ? body.string() : "空响应体";
                        callback.onFailure(new Exception("request failed:" + response.code() + "," + errorBody));
                        return;
                    }
                    BufferedSource source = body.source();
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) {
                            continue;
                        }
                        if (line.startsWith("data:")) {
                            String jsonData = line.substring(6).trim();
                            callback.onResponse(jsonData);
                            if (jsonData.equals("[DONE]")) {
                                //流结束
                                break;
                            }
                        } else if (StrUtil.isNotEmpty(line.trim())) {
                            callback.onFailure(new Exception("illegal data: " + line));
                            break;
                        }
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    private static OkHttpClient getClient() {
        if (okHttpClient == null) {
            Integer i = SpringBeanUtil.getBean(LLMProperties.class).getTimeout();
            if (ObjectUtil.isNotEmpty(i)) {
                timeout = i;
            }
            //100个连接,空闲连接保持3分钟
            ConnectionPool connectionPool = new ConnectionPool(100, 3, TimeUnit.MINUTES);

            // 创建自定义Dispatcher，增加并发限制
            okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher();
            // 总体最大请求数
            dispatcher.setMaxRequests(200);
            // 每个主机最大请求数,超过该值,会进入队列等待
            dispatcher.setMaxRequestsPerHost(50);

            okHttpClient = new OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    //建立TCP连接的最大等待时间
                    .connectTimeout(Duration.ofSeconds(15))
                    //写入请求数据到服务器的超时时间
                    .writeTimeout(Duration.ofSeconds(30))
                    //从服务器读取响应数据的超时时间
                    .readTimeout(Duration.ofSeconds(30))
                    //整个http调用的总超时时间
                    .callTimeout(Duration.ofSeconds(timeout))
                    .connectionPool(connectionPool)
                    .build();
        }
        return okHttpClient;
    }
}
