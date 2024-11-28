package com.litevar.agent.base.constant;

/**
 * @author uncle
 * @since 2024/7/31 12:13
 */
public interface CacheKey {

    String LOGIN_TOKEN = "login:token:%s";
    /**
     * 激活账号信息
     */
    String ACTIVATE_USER_INFO = "activate:info:%s";

    String USER_INFO = "user:info";

    /**
     * 用户角色
     */
    String USER_ROLE = "user:role";

    String USER_WORKSPACE_ID = "user:workspaceId";

    /**
     * 系统初始化状态
     */
    String INIT_STATUS = "initStatus";

    String SESSION_INFO = "session:info:%s";
    String TOKEN_USAGE = "session:tokenUsage:%s";
    String TASK_MESSAGE = "session:taskMessage:%s";
    String TOOL_API_KEY = "session:apiKey:%s:%s";
    String SESSION_FUNCTION_INFO = "session:function:%s:%s";
    String SESSION_CHAT = "session:chat:%s";

    /**
     * agent未发布的信息
     */
    String AGENT_DRAFT = "agent:draft:%s";

    String MODEL_INFO = "model:info";
    String TOOL_INFO = "tool:info";
}