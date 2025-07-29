package com.litevar.agent.openai;

/**
 * @author uncle
 * @since 2025/2/19 10:14
 */
public interface RequestCallback {

    void onResponse(String response);

    void onFailure(Throwable throwable);
}
