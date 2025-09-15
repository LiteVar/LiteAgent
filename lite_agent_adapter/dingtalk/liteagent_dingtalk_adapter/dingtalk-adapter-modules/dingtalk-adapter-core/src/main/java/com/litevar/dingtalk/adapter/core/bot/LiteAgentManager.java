package com.litevar.dingtalk.adapter.core.bot;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.dingtalk.adapter.common.core.exception.ServiceException;
import com.litevar.liteagent.client.LiteAgentClient;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.HttpServerErrorException;
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
        try {
        if (StrUtil.isBlank(cacheSessionId)) {
            cacheSessionId = client.initSession();
            mapCache.put(conversationId, cacheSessionId, 1,
                    TimeUnit.HOURS);
        }
        String sessionId = cacheSessionId;
        ApiRecords.ChatRequest request = new ApiRecords.ChatRequest(List.of(
                new ApiRecords.ContentListItem("text", msg)), isChunk);
        Flux<ServerSentEvent<ApiRecords.AgentMessage>> response = client.chat(sessionId, request, messageHandler);

            response
                    .doOnComplete(onComplete)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnError(e -> {
                        handleLiteAgentError(e, client, conversationId, request, messageHandler, onComplete);
                    })
                    .blockLast(Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("请求liteAgent异常: {}", e.getMessage());
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            if(e instanceof HttpServerErrorException){
                throw new ServiceException("暂时无法连接到服务器,请稍后再试!");
            }

        }

    }

    /**
     * 处理LiteAgent错误
     *
     * @param e              异常
     * @param client         客户端
     * @param conversationId 会话ID
     * @param request        请求
     * @param messageHandler 消息处理器
     * @param onComplete     完成回调
     */
    private void handleLiteAgentError(Throwable e, LiteAgentClient client, String conversationId,
                                      ApiRecords.ChatRequest request, MessageHandler messageHandler, Runnable onComplete) {
        String errorMessage = e.getMessage();
        // 如果e是WebException类型，您可以从中提取更多信息webEx.getResponseBodyAsString()不为空则是liteagent返回的异常
        if (e instanceof WebClientResponseException webEx && StrUtil.isNotBlank(webEx.getResponseBodyAsString())) {
            JSONObject jsonObject = JSONUtil.parseObj(webEx.getResponseBodyAsString());
            errorMessage = jsonObject.getStr("message");
            Integer code = jsonObject.getInt("code");

            // 错误代码30002需要重新初始化session并重试
            if (ObjUtil.isNotEmpty(code) && code.equals(30002)) {
                log.warn("检测到错误代码30002，重新初始化session并重试");
                // 重新初始化session
                String newSessionId = client.initSession();
                mapCache.put(conversationId, newSessionId, 1, TimeUnit.HOURS);
                Flux<ServerSentEvent<ApiRecords.AgentMessage>> newResponse = client.chat(newSessionId, request, messageHandler);
                newResponse
                        .doOnComplete(onComplete)
                        .doOnError(e1 -> {
                            log.error("重试后仍然发生错误: {}", e1.getMessage());
                            throw new ServiceException(extractErrorMessage(e1));
                        })
                        .blockLast(Duration.ofMinutes(5));
            } else {
                throw new ServiceException(errorMessage);
            }
        } else {
            log.error("请求liteAgent异常: {}", errorMessage);
            throw new ServiceException("暂时无法连接到服务器,请稍后再试!");
        }
    }

    /**
     * 提取错误信息
     *
     * @param e 异常
     * @return 错误信息
     */
    private String extractErrorMessage(Throwable e) {
        String errorMessage;
        if (e instanceof WebClientResponseException webEx && StrUtil.isNotBlank(webEx.getResponseBodyAsString())) {
            JSONObject jsonObject = JSONUtil.parseObj(webEx.getResponseBodyAsString());
            errorMessage = jsonObject.getStr("message");
        }else {
            errorMessage = "暂时无法连接到服务器,请稍后再试!";
        }
        return errorMessage;
    }

    /**
     * 清除会话
     *
     * @param conversationId 会话id
     */
    public void cleanSession(String conversationId) {
        mapCache.remove(conversationId);
    }


}