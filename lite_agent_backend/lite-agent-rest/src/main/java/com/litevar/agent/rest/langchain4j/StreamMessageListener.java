package com.litevar.agent.rest.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

/**
 * @author uncle
 * @since 2024/10/15 16:00
 */
public interface StreamMessageListener {
    void onComplete(Response<AiMessage> message);

    void onError(Throwable e);

    void onNext(String part);

    String callFunction(ToolExecutionRequest toolExecutionRequest);
}