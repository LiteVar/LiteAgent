package com.litevar.agent.core.module.plugin;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.storage.SecretKeyService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin runner client.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Component
public class PluginRunnerClient {
    private static final String URL_HEALTH = "/runner/health";
    private static final String URL_UPLOAD_PACKAGE = "/runner/plugins/%s/package";
    private static final String URL_ENABLE = "/runner/plugins/%s/enable";
    private static final String URL_DISABLE = "/runner/plugins/%s/disable";
    private static final String URL_STATUS = "/runner/plugins/%s/status";

    @Value("${plugin.runner.base-url:}")
    private String baseUrl;
    @Value("${plugin.runner.encrypted-key:}")
    private String encryptedKey;


    private final SecretKeyService secretKeyService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private volatile byte[] runnerSharedKey;

    public PluginRunnerClient(SecretKeyService secretKeyService,
                              WebClient webClient,
                              ObjectMapper objectMapper) {
        this.secretKeyService = secretKeyService;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
    }

    public RunnerHealth health() {
        return webClient.get()
                .uri(baseUrl() + URL_HEALTH)
                .retrieve()
                .bodyToMono(RunnerHealth.class)
                .block();
    }

    public void ensurePaired() {
        RunnerHealth health = health();
        if (health == null) {
            throw new ServiceException(ServiceExceptionEnum.RUNNER_NOT_AVAILABLE);
        }
        if (!health.isDockerOk()) {
            throw new ServiceException(ServiceExceptionEnum.DOCKER_NOT_AVAILABLE);
        }
        if (!health.isPaired()) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "runner密钥未就绪");
        }
        if (runnerSharedKey == null) {
            if (StrUtil.isBlank(encryptedKey)) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "runner密钥未配置");
            }
            try {
                runnerSharedKey = secretKeyService.decrypt(encryptedKey);
            } catch (Exception ex) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "runner密钥解密失败");
            }
        }
    }

    /**
     * 转发插件包
     */
    public void uploadPackage(String pluginId, MultiValueMap<String, HttpEntity<?>> body) {
        ensurePaired();
        String path = String.format(URL_UPLOAD_PACKAGE, pluginId);
        webClient.post()
                .uri(baseUrl() + path)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> toException(response, "runner 上传失败"))
                .bodyToMono(Void.class)
                .block();
    }

    public RunnerEnableResponse enable(String pluginId, String packageUrl) {
        ensurePaired();
        String path = String.format(URL_ENABLE, pluginId);
        Map<String, String> body = null;
        if (StrUtil.isNotBlank(packageUrl)) {
            body = new HashMap<>();
            body.put("packageUrl", packageUrl);
        }
        return postJson(path, null, body, true, RunnerEnableResponse.class);
    }

    public void disable(String pluginId) {
        ensurePaired();
        String path = String.format(URL_DISABLE, pluginId);
        postJson(path, null, null, true, Void.class);
    }

    public RunnerStatusResponse status(String pluginId) {
        //获取插件容器运行状态
        ensurePaired();
        String path = String.format(URL_STATUS, pluginId);
        return getJson(path, null, true, RunnerStatusResponse.class);
    }

    private <T> T getJson(String path, Map<String, List<String>> query, boolean sign, Class<T> responseType) {
        String queryString = PluginAuthUtil.normalizeQuery(query);
        String url = baseUrl() + path + (StrUtil.isNotEmpty(queryString) ? "?" + queryString : "");
        WebClient.RequestHeadersSpec<?> request = webClient.method(HttpMethod.GET)
                .uri(url);
        if (sign) {
            applySignHeaders(request, "GET", path, queryString, null, "");
        }
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, response -> toException(response, "runner 请求失败"))
                .bodyToMono(responseType)
                .block();
    }

    private <T> T postJson(String path, Map<String, List<String>> query, Object body, boolean sign,
                           Class<T> responseType) {
        String queryString = PluginAuthUtil.normalizeQuery(query);
        String url = baseUrl() + path + (StrUtil.isNotEmpty(queryString) ? "?" + queryString : "");
        byte[] bodyBytes = new byte[0];
        if (body != null) {
            try {
                bodyBytes = objectMapper.writeValueAsBytes(body);
            } catch (Exception ex) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "请求体序列化失败");
            }
        }
        WebClient.RequestBodySpec request = webClient.method(HttpMethod.POST)
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);
        if (sign) {
            applySignHeaders(request, "POST", path, queryString, bodyBytes, MediaType.APPLICATION_JSON_VALUE);
        }
        return request.bodyValue(bodyBytes)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> toException(response, "runner 请求失败"))
                .bodyToMono(responseType)
                .block();
    }

    private void applySignHeaders(WebClient.RequestHeadersSpec<?> request, String method, String path, String query,
                                  byte[] bodyBytes, String contentType) {
        PluginAuthUtil.SignedHeaders headers = PluginAuthUtil.sign(runnerSharedKey, method, path, query, bodyBytes, contentType);
        request.header("X-TS", headers.ts());
        request.header("X-Nonce", headers.nonce());
        request.header("X-Sign", headers.sign());
    }

    private Mono<? extends Throwable> toException(ClientResponse response, String fallback) {
        return response.bodyToMono(Map.class)
                .map(body -> new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        extractErrorMessage(body, fallback)))
                .onErrorReturn(new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), fallback));
    }

    private String extractErrorMessage(Map<?, ?> body, String fallback) {
        if (body == null) {
            return fallback;
        }
        Object message = body.get("message");
        Object detail = body.get("detail");
        if (message == null && detail == null) {
            return fallback;
        }
        if (detail == null) {
            return String.valueOf(message);
        }
        return message + ":" + detail;
    }

    private String baseUrl() {
        if (StrUtil.isBlank(baseUrl)) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "runner 地址未配置");
        }
        return baseUrl;
    }

    @Getter
    @Setter
    public static class RunnerHealth {
        private boolean paired;
        private boolean dockerOk;
    }

    @Setter
    @Getter
    public static class RunnerEnableResponse {
        private String containerId;
        private Integer hostPort;
    }

    @Setter
    @Getter
    public static class RunnerStatusResponse {
        private String containerId;
        private boolean running;
        private Integer hostPort;

    }
}
