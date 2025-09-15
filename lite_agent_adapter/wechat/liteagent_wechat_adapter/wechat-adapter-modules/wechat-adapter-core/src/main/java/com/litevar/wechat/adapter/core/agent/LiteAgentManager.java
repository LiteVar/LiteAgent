package com.litevar.wechat.adapter.core.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.liteagent.client.LiteAgentClient;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import com.litevar.wechat.adapter.common.core.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Teoan
 * @since 2025/8/14 17:15
 */
@Slf4j
@AllArgsConstructor
public class LiteAgentManager {

    private RMapCache<String, String> mapCache;
    private String baseUrl;
    private String apiKey;

    /**
     * 发送LiteAgent消息
     *
     * @param msg            消息
     * @param messageHandler 消息处理器
     * @param conversationId 会话id
     */
    public void sendLiteAgentMessage(String msg, String conversationId, Boolean isChunk, MessageHandler messageHandler, Runnable onComplete) {
        log.debug("send agent message:{}", msg);
        LiteAgentClient client = new LiteAgentClient(baseUrl, apiKey);
        // 缓存会话id 保留上下文，缓存时间和liteagent一致
        String cacheSessionId = mapCache.get(conversationId);
        if (StrUtil.isBlank(cacheSessionId)) {
            cacheSessionId = client.initSession();
            mapCache.put(conversationId, cacheSessionId, 1,
                    TimeUnit.HOURS);
        }
        String sessionId = cacheSessionId;
        ApiRecords.ChatRequest request = new ApiRecords.ChatRequest(List.of(
                new ApiRecords.ContentListItem("text", msg)), isChunk);
        Flux<ServerSentEvent<ApiRecords.AgentMessage>> response = client.chat(sessionId, request, messageHandler);
        try {
            response
                    .doOnComplete(onComplete)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnError(e -> {
                        String errorMessage = e.getMessage();
                        log.error("请求liteAgent异常: {}", errorMessage);
                        // 如果e是WebException类型，您可以从中提取更多信息
                        if (e instanceof WebClientResponseException) {
                            WebClientResponseException webEx = (WebClientResponseException) e;
                            JSONObject jsonObject = JSONUtil.parseObj(webEx.getResponseBodyAsString());
                            errorMessage = jsonObject.getStr("message");
                            Integer code = jsonObject.getInt("code");
                            if (code.equals(30002)) {
                                log.warn("检测到错误代码30002，重新初始化session并重试");
                                // 重新初始化session
                                String newSessionId = client.initSession();
                                mapCache.put(conversationId, newSessionId, 1, TimeUnit.HOURS);
                                Flux<ServerSentEvent<ApiRecords.AgentMessage>> newResponse = client.chat(newSessionId, request, messageHandler);
                                newResponse
                                        .doOnComplete(onComplete)
                                        .doOnError(e1 -> {
                                            log.error("重试后仍然发生错误: {}", e1.getMessage());
                                            String errorMessage1 = e1.getMessage();
                                            // 如果e是WebException类型，您可以从中提取更多信息
                                            if (e1 instanceof WebClientResponseException webEx1) {
                                                JSONObject jsonObject1 = JSONUtil.parseObj(webEx1.getResponseBodyAsString());
                                                errorMessage1 = jsonObject1.getStr("message");
                                            }
                                            throw new ServiceException(errorMessage1);
                                        })
                                        .blockLast(Duration.ofMinutes(5));
                            } else {
                                throw new ServiceException(errorMessage);
                            }
                        }
                    })
                    .blockLast(Duration.ofMinutes(5));
        } catch (Exception e) {
            // 只抛出ServiceException
            log.error("请求liteAgent异常: {}", e.getMessage());
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
        }
    }


    /**
     * 清除会话
     *
     * @param conversationId 会话id
     */
    public void cleanSession(String conversationId) {
        log.debug("clean session:{}", conversationId);
        mapCache.remove(conversationId);
    }


}
