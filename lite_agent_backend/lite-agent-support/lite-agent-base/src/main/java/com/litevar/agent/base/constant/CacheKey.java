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
    String TOOL_API_KEY = "session:apiKey:%s:%s";
    String SESSION_FUNCTION_INFO = "session:function:%s:%s";
    String SESSION_CHAT = "session:chat:%s";
    String REFLECT_INFO = "session:reflect:%s";
    String REFLECT_TOOL_INFO = "session:reflect:tool:%s";

    /**
     * agent未发布的信息
     */
    String AGENT_DRAFT = "agent:draft:%s";
    String AGENT_DATASET_DRAFT = "agent:draft:dataset:%s";
    String AGENT_API_KEY = "agent:apiKey";

    String MODEL_INFO = "model:info";
    String TOOL_INFO = "tool:info";

    // 重置密码-验证码缓存key
    String RESET_PASSWORD_CAPTCHA = "reset:password:captcha:%s";

}