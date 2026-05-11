package com.litevar.agent.core.module.plugin;

import cn.hutool.crypto.SecureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.entity.Plugin;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.storage.SecretKeyService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Plugin container client.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Component
public class PluginContainerClient {
    private static final String URL_SCHEMA = "/plugin/schema";
    private static final String URL_DATA = "/plugin/data";
    private static final String URL_CONFIG = "/plugin/config";
    private static final String URL_STATUS = "/plugin/status";
    private static final String URL_DELETE = "/plugin/delete";
    private static final String URL_ANALYZE = "/plugin/analyze";

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final SecretKeyService secretKeyService;

    @Lazy
    @Resource
    private PluginService pluginService;

    public PluginContainerClient(WebClient webClient,
                                 ObjectMapper objectMapper,
                                 SecretKeyService secretKeyService) {
        this.objectMapper = objectMapper;
        this.webClient = webClient;
        this.secretKeyService = secretKeyService;
    }

    public Object schema(String pluginId) {
        ResolvedPlugin resolved = resolvePlugin(pluginId);
        return getJson(resolved.baseUrl(), URL_SCHEMA, null, resolved.sharedKey());
    }

    public Object data(String pluginId, String connectorId) {
        ResolvedPlugin resolved = resolvePlugin(pluginId);
        Map<String, List<String>> query = Map.of("connectorId", List.of(connectorId));
        return getJson(resolved.baseUrl(), URL_DATA, query, resolved.sharedKey());
    }

    public Map<String, Object> config(String pluginId, String connectorId, Object data) {
        ResolvedPlugin resolved = resolvePlugin(pluginId);
        Map<String, Object> body = new HashMap<>();
        body.put("connectorId", connectorId);
        body.put("data", data);
        return postJson(resolved.baseUrl(), URL_CONFIG, null, body, true, Map.class, resolved.sharedKey());
    }

    public Map<String, Object> status(String pluginId, String connectorId, boolean offline) {
        ResolvedPlugin resolved = resolvePlugin(pluginId);
        Map<String, Object> body = new HashMap<>();
        body.put("connectorId", connectorId);
        body.put("offline", offline);
        return postJson(resolved.baseUrl(), URL_STATUS, null, body, true, Map.class, resolved.sharedKey());
    }

    public Map<String, Object> delete(String pluginId, String connectorId) {
        ResolvedPlugin resolved = resolvePlugin(pluginId);
        Map<String, Object> body = new HashMap<>();
        body.put("connectorId", connectorId);
        return postJson(resolved.baseUrl(), URL_DELETE, null, body, true, Map.class, resolved.sharedKey());
    }

    public Object analyze(String pluginId, String connectorId, String startTime, String endTime) {
        ResolvedPlugin resolved = resolvePlugin(pluginId);
        Map<String, Object> body = new HashMap<>();
        body.put("connectorId", connectorId);
        body.put("startTime", startTime);
        body.put("endTime", endTime);
        return postJson(resolved.baseUrl(), URL_ANALYZE, null, body, true, Object.class, resolved.sharedKey());
    }

    private Object getJson(String baseUrl, String path, Map<String, List<String>> query, byte[] sharedKey) {
        String queryString = PluginAuthUtil.normalizeQuery(query);
        String url = buildUrl(baseUrl, path, queryString);
        WebClient.RequestHeadersSpec<?> request = webClient.method(HttpMethod.GET)
                .uri(url);
        applySignHeaders(request, "GET", path, queryString, null, "", sharedKey);
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, response -> toException(response, "插件容器请求失败"))
                .bodyToMono(Object.class)
                .block();
    }

    private <T> T postJson(String baseUrl, String path, Map<String, List<String>> query, Object body, boolean sign,
                           Class<T> responseType, byte[] sharedKey) {
        String queryString = PluginAuthUtil.normalizeQuery(query);
        String url = buildUrl(baseUrl, path, queryString);
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
            applySignHeaders(request, "POST", path, queryString, bodyBytes, MediaType.APPLICATION_JSON_VALUE, sharedKey);
        }
        return request.bodyValue(bodyBytes)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> toException(response, "插件容器请求失败"))
                .bodyToMono(responseType)
                .block();
    }

    private void applySignHeaders(WebClient.RequestHeadersSpec<?> request, String method, String path, String query,
                                  byte[] bodyBytes, String contentType, byte[] sharedKey) {
        PluginAuthUtil.SignedHeaders headers = PluginAuthUtil.sign(sharedKey, method, path, query, bodyBytes, contentType);
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
        if (body == null || body.get("message") == null) {
            return fallback;
        }
        return body.get("message").toString();
    }

    private String buildUrl(String baseUrl, String path, String queryString) {
        String url = baseUrl + path;
        return StringUtils.hasText(queryString) ? url + "?" + queryString : url;
    }

    private ResolvedPlugin resolvePlugin(String pluginId) {
        Plugin plugin = pluginService.findById(pluginId);
        Callable<String> task = () -> SecureUtil.md5(SecretKeyService.secret + ":" + pluginId).toLowerCase().substring(0, 16);
        String encryptedSecretKey = null;
        try {
            encryptedSecretKey = secretKeyService.getEncryptedSecretKey(SecretKeyService.PLUGIN_KEY_PREFIX + pluginId, task);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] sharedKey = secretKeyService.decrypt(encryptedSecretKey);
        return new ResolvedPlugin(plugin.getUrl(), sharedKey);
    }

    private record ResolvedPlugin(String baseUrl, byte[] sharedKey) {
    }
}
