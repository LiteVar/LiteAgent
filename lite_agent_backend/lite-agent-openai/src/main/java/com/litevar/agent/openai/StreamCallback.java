package com.litevar.agent.openai;

import com.litevar.agent.openai.completion.CompletionResponse;

/**
 * @author uncle
 * @since 2025/2/20 11:45
 */
public interface StreamCallback {

    void onPartialResponse(String part);

    void onError(Throwable error);

    void onCompleteResponse(CompletionResponse response);


    void start();
}