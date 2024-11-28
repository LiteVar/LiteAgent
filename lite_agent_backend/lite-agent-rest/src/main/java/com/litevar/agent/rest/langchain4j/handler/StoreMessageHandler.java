package com.litevar.agent.rest.langchain4j.handler;

import cn.hutool.core.util.IdUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.AgentChatMessage;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.core.event.AgentMessageEvent;
import com.litevar.agent.rest.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息持久化处理
 *
 * @author uncle
 * @since 2024/11/19 10:10
 */
public class StoreMessageHandler implements AiMessageHandler {
    private final List<OutMessage> allMessage = new ArrayList<>();
    private final String sessionId;

    public StoreMessageHandler(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String role() {
        return "STORE";
    }

    @Override
    public void onSend(OutMessage userMessage) {
        allMessage.add(userMessage);
    }

    @Override
    public void onComplete(Response<AiMessage> message) {
        String taskId = IdUtil.fastSimpleUUID();
        OutMessage aiMessage = handleAssistantMessage(message);
        allMessage.add(aiMessage);
        RedisUtil.setListValue(String.format(CacheKey.TASK_MESSAGE, taskId), allMessage.toArray(new OutMessage[0]));
        allMessage.clear();
        AgentChatMessage.TokenUsage usage = handleTokenUsage(message);
        RedisUtil.setValue(String.format(CacheKey.TOKEN_USAGE, taskId), usage, 1L, TimeUnit.HOURS);

        SpringUtil.publishEvent(new AgentMessageEvent(this, sessionId, taskId));
    }

    @Override
    public void onError(Throwable e) {
        allMessage.clear();
    }

    @Override
    public void onNext(String part) {

    }

    @Override
    public void callFunction(OutMessage functionCallMessage) {
        allMessage.add(functionCallMessage);
    }

    @Override
    public void functionResult(OutMessage toolResultMessage) {
        allMessage.add(toolResultMessage);
    }

    private OutMessage handleAssistantMessage(Response<AiMessage> message) {
        OutMessage outMessage = new OutMessage();
        outMessage.setRole("assistant");
        outMessage.setType("text");
        outMessage.setContent(message.content().text());
        return outMessage;
    }

    private AgentChatMessage.TokenUsage handleTokenUsage(Response<AiMessage> message) {
        TokenUsage tokenUsage = message.tokenUsage();
        AgentChatMessage.TokenUsage usage = new AgentChatMessage.TokenUsage();
        usage.setPromptTokens(tokenUsage.inputTokenCount());
        usage.setCompletionTokens(tokenUsage.outputTokenCount());
        usage.setTotalTokens(tokenUsage.totalTokenCount());
        return usage;
    }
}
