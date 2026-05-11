package com.litevar.agent.openai;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.llm.LlmRequestReporter;
import com.litevar.agent.openai.completion.CompletionRequestParam;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.util.LLMProperties;
import com.litevar.agent.openai.util.SpringBeanUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String BEARER_PREFIX = "Bearer ";
    private static WebClient webClient;
    private static final Pattern thinkPattern = Pattern.compile("<think>(.*?)</think>");
    private static LlmRequestReporter llmRequestReporter;
    private static final AtomicBoolean reporterInitialized = new AtomicBoolean(false);

    private RequestExecutor() {
    }

    public static CompletionResponse doRequest(CompletionRequestParam param, String baseUrl, String key) {
        CallHandle<CompletionResponse> handle = doRequestAsync(param, baseUrl, key, response -> {
        });
        try {
            return handle.future().join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause != null) {
                throw new RuntimeException(cause);
            }
            throw ex;
        }
    }

    /**
     * stream=false
     */
    public static CallHandle<CompletionResponse> doRequestAsync(CompletionRequestParam param, String baseUrl, String key,
                                                                Consumer<CompletionResponse> afterResponse) {
        CallHandle<CompletionResponse> handle = new CallHandle<>();
        if (checkBalanceIfPresent(param.getTokenReport(), handle) != null) {
            return handle;
        }
        requestInternal(buildRequestPayload(param, key, baseUrl), 1, false, handle,
                response -> {
                    try {
                        CompletionResponse res = ObjectMapperSingleton.getObjectMapper().readValue(response, CompletionResponse.class);
                        //report token usage
                        reportUsageIfPresent(param.getTokenReport(), res.getUsage());
                        AssistantMessage assistantMessage = res.getChoices().get(0).getMessage();
                        if (StrUtil.isNotEmpty(assistantMessage.getReasoning())) {
                            //兼容thinking输出用reasoning字段
                            assistantMessage.setReasoningContent(assistantMessage.getReasoning());
                            assistantMessage.setReasoning(null);
                        }
                        if (StrUtil.isNotEmpty(assistantMessage.getContent())) {
                            String content = assistantMessage.getContent();
                            Matcher matcher = thinkPattern.matcher(content);
                            if (matcher.find()) {
                                //content字段包含<think></think>的情况
                                assistantMessage.setReasoningContent(matcher.group(1));
                                assistantMessage.setContent(content.replaceAll("<think>.*?</think>", ""));
                            }
                        }
                        afterResponse.accept(res);
                        handle.future().complete(res);
                    } catch (Exception e) {
                        handle.future().completeExceptionally(e);
                    }
                },
                throwable -> handle.future().completeExceptionally(throwable));
        return handle;
    }

    /**
     * stream=true
     */
    public static CallHandle<Void> doStreamRequest(CompletionRequestParam param, String baseUrl,
                                                   String key, RequestCallback callback) {
        CallHandle<Void> handle = new CallHandle<>();
        Throwable balanceError = checkBalanceIfPresent(param.getTokenReport(), handle);
        if (balanceError != null) {
            callback.onFailure(balanceError);
            return handle;
        }
        requestInternal(buildRequestPayload(param, key, baseUrl), 1, true, handle,
                res -> {
                    if (StrUtil.isEmpty(res)) {
                        return;
                    }
                    String payload = res;
                    if (payload.startsWith("data:")) {
                        payload = payload.substring(5);
                    }
                    callback.onResponse(payload);
                    if ("[DONE]".equals(payload.trim())) {
                        Disposable current = handle.disposable.get();
                        if (current != null) {
                            current.dispose();
                        }
                        handle.future().complete(null);
                    }
                },
                throwable -> {
                    callback.onFailure(throwable);
                    handle.future().completeExceptionally(throwable);
                });
        return handle;
    }

    private static void requestInternal(RequestPayload requestPayload,
                                        int attempt, boolean stream,
                                        CallHandle<?> handle,
                                        Consumer<String> afterResponse,
                                        Consumer<Throwable> afterError) {
        if (handle.isCancelled()) {
            return;
        }
        //响应
        Consumer<String> response = res -> {
            if (handle.isCancelled()) {
                return;
            }
            afterResponse.accept(res);
        };
        //异常
        Consumer<Throwable> error = throwable -> {
            if (handle.isCancelled()) {
                return;
            }

            Throwable toReport = throwable;
            if (throwable instanceof WebClientResponseException ex) {
                String body = ex.getResponseBodyAsString();
                toReport = new RuntimeException("request failed:" + ex.getStatusCode().value() + "," + (StrUtil.isNotEmpty(body) ? body : "empty response"), ex);
            }
            if (attempt <= maxAttempts) {
                String text = "Exception was thrown on attempt %s of %s".formatted(attempt, maxAttempts);
                log.warn(text, throwable);
                RetryUtil.sleep(attempt);
                // 重试
                requestInternal(requestPayload, attempt + 1, stream, handle, afterResponse, afterError);
                return;
            }
            afterError.accept(toReport);
        };

        WebClient.ResponseSpec spec = buildRequest(requestPayload, stream).retrieve();
        Disposable disposable;
        if (stream) {
            disposable = spec.bodyToFlux(String.class)
                    .subscribe(response, error);
        } else {
            disposable = spec.bodyToMono(String.class)
                    .subscribe(response, error);
        }
        handle.setDisposable(disposable);
    }

    private static WebClient.RequestHeadersSpec<?> buildRequest(RequestPayload requestPayload, boolean stream) {
        WebClient.RequestBodySpec spec = getWebClient().post()
                .uri(requestPayload.baseUrl + chatCompletion)
                .headers(httpHeaders -> requestPayload.headers.forEach(httpHeaders::add))
                .contentType(MediaType.APPLICATION_JSON);
        if (stream) {
            spec.accept(MediaType.TEXT_EVENT_STREAM);
        }
        return spec.bodyValue(requestPayload.data);
    }

    private static RequestPayload buildRequestPayload(CompletionRequestParam param, String key, String baseUrl) {
        Map<String, String> headers = new HashMap<>(3);
        if (StrUtil.isNotEmpty(key)) {
            headers.put(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + key);
        }
        String data;
        try {
            data = ObjectMapperSingleton.getObjectMapper().writeValueAsString(param);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new RequestPayload(headers, data, baseUrl);
    }

    private record RequestPayload(Map<String, String> headers, String data, String baseUrl) {
    }

    public static void reportUsageIfPresent(TokenReportDTO tokenReport, CompletionResponse.Usage usage) {
        if (tokenReport == null || usage == null) {
            return;
        }
        LlmRequestReporter reporter = getLlmRequestReporter();
        if (reporter == null) {
            return;
        }
        try {
            reporter.report(tokenReport.userId(), tokenReport.modelId(), tokenReport.agentId(), usage.getPromptTokens(), usage.getCompletionTokens());
        } catch (Exception e) {
            log.warn("token usage上报失败", e);
        }
    }

    private static LlmRequestReporter getLlmRequestReporter() {
        if (reporterInitialized.get()) {
            return llmRequestReporter;
        }
        if (reporterInitialized.compareAndSet(false, true)) {
            Map<String, LlmRequestReporter> reporters = SpringBeanUtil.getBeansOfType(LlmRequestReporter.class);
            llmRequestReporter = reporters.isEmpty() ? null : reporters.values().iterator().next();
        }
        return llmRequestReporter;
    }

    private static Throwable checkBalanceIfPresent(TokenReportDTO tokenReport, CallHandle<?> handle) {
        if (tokenReport == null) {
            return null;
        }
        LlmRequestReporter reporter = getLlmRequestReporter();
        if (reporter == null) {
            return null;
        }
        try {
            reporter.checkBalance(tokenReport.userId(), tokenReport.modelId());
            return null;
        } catch (Exception e) {
            handle.future().completeExceptionally(e);
            return e;
        }
    }

    private static WebClient getWebClient() {
        if (webClient == null) {
            Integer i = SpringBeanUtil.getBean(LLMProperties.class).getTimeout();
            if (ObjectUtil.isNotEmpty(i)) {
                timeout = i;
            }
            ConnectionProvider provider = ConnectionProvider.builder("openai-connection-pool")
                    .maxConnections(100)
                    .maxIdleTime(Duration.ofMinutes(3))
                    .build();
            HttpClient httpClient = HttpClient.create(provider)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000)
                    .responseTimeout(Duration.ofSeconds(timeout))
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(30))
                            .addHandlerLast(new WriteTimeoutHandler(30)));
            webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        }
        return webClient;
    }

    public static class CallHandle<T> {
        private final CompletableFuture<T> future;
        private final AtomicReference<Disposable> disposable = new AtomicReference<>();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private CallHandle() {
            future = new CompletableFuture<>();
        }

        public CompletableFuture<T> future() {
            return future;
        }

        private void setDisposable(Disposable disposable) {
            this.disposable.set(disposable);
        }

        public void cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            Disposable current = disposable.get();
            if (current != null) {
                current.dispose();
            }
            future.completeExceptionally(new CancellationException("stopped"));
        }

        private boolean isCancelled() {
            return cancelled.get();
        }
    }
}
