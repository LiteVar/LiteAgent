package com.litevar.agent.rest.langchain4j.handler;

import com.litevar.agent.base.vo.OutMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

/**
 * 消息处理
 *
 * @author uncle
 * @since 2024/11/18 16:58
 */
public interface AiMessageHandler {

    String role();

    void onSend(OutMessage userMessage);

    void onComplete(Response<AiMessage> message);

    void onError(Throwable e);

    void onNext(String part);

    void callFunction(OutMessage functionCallMessage);

    void functionResult(OutMessage toolResultMessage);
}