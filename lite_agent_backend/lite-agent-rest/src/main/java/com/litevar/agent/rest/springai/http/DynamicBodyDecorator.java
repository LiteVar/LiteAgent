package com.litevar.agent.rest.springai.http;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 拦截WebClient 发送的请求体, 对请求体参数进行动态重命名。
 * @author reid
 * @since 2025/6/27
 */

@Slf4j
public class DynamicBodyDecorator extends ClientHttpRequestDecorator {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> fieldMapping;

    public DynamicBodyDecorator(ClientHttpRequest delegate, Map<String, String> fieldMapping) {
        super(delegate);
        this.fieldMapping = fieldMapping;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        // 将原始的 body (可能是分块的) 连接成一个单独的 DataBuffer
        Mono<DataBuffer> modifiedBody = DataBufferUtils.join(body)
            .map(dataBuffer -> {
                // 执行 JSON 字段的重命名逻辑
                byte[] modifiedBytes = modifyJsonPayload(dataBuffer);
                // 用修改后的字节创建一个新的 DataBuffer
                return getDelegate().bufferFactory().wrap(modifiedBytes);
            });

        // 将包含修改后内容的新 Publisher 写回父类，由父类完成网络发送
        return super.writeWith(modifiedBody);
    }

    private byte[] modifyJsonPayload(DataBuffer dataBuffer) {
        String jsonString = dataBuffer.toString(StandardCharsets.UTF_8);
        DataBufferUtils.release(dataBuffer); // 释放原始 buffer

        try {
            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
            this.fieldMapping.forEach((key, value) -> {
                if (jsonNode.has(key)) {
                    // 完整地获取原始字段的 JsonNode，以保留其原始数据类型（字符串、数字、对象等）
                    JsonNode valueNode = jsonNode.get(key);

                    // 将该节点设置到新字段名下
                    jsonNode.set(value, valueNode);

                    // 移除原始字段
                    jsonNode.remove(key);
                }
            });
            return objectMapper.writeValueAsBytes(jsonNode);
        } catch (JsonProcessingException e) {
            log.error("JSON解析或序列化失败: {}", e.getMessage(), e);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        }
    }
}
