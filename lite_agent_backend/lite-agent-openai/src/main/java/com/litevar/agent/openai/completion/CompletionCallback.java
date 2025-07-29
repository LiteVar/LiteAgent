package com.litevar.agent.openai.completion;

/**
 * @author uncle
 * @since 2025/3/5 17:32
 */
public interface CompletionCallback {
    /**
     * chunkType: 0-大模型总结输出内容,1-思考内容
     */
    void onPartialResponse(String taskId, String part, Integer chunkType);

    void onError(String taskId, Throwable error);

    void onCompleteResponse(String taskId, CompletionResponse response, boolean isStream);

    void start(String taskId);
}