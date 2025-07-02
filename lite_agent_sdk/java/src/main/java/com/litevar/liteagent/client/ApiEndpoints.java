package com.litevar.liteagent.client;


/**
 * @author reid
 * @since 2025/6/18
 */

public interface ApiEndpoints {
    /**
     * Base URL for the LiteAgent API.
     */
    String BASE_URL = "https://api.liteagent.cn/liteAgent/v1";
    /**
     * get the version of the LiteAgent service.
     */
    String GET_VERSION = "/version";
    /**
     * initialize a session
     */
    String INIT_SESSION = "/initSession";
    /**
     * chat api endpoint.
     */
    String CHAT = "/chat";
    /**
     * callback endpoint for client exec function call result
     */
    String CALLBACK = "/callback";
    /**
     * chat chatHistory endpoint.
     */
    String HISTORY = "/history";
    /**
     * stop a chat session.
     */
    String STOP = "/stop";
    /**
     * clear chat session.
     */
    String CLEAR = "/clear";
}
