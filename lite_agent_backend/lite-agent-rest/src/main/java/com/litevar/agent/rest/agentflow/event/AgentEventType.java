package com.litevar.agent.rest.agentflow.event;

/**
 * 事件类型
 *
 * @author uncle
 * @since 2025/12/18 15:01
 */
public enum AgentEventType {

    USER_SEND_EVENT("用户发送消息"),

    LLM_EVENT("LLM响应内容"),

    THINK_EVENT("LLM思考内容"),

    ERROR_EVENT("异常"),

    CHUNK_EVENT("stream流消息片段"),

    FUNCTION_CALL_EVENT("function-calling"),

    OPEN_TOOL_CALL_EVENT("open-tool 第三方工具调用"),

    TOOL_RESULT_EVENT("tool result"),

    REFLECTION_EVENT("反思"),

    AGENT_DISPATCH_EVENT("子agent调度"),

    KNOWLEDGE_EVENT("知识库调用"),

    AGENT_SWITCH_EVENT("agent切换"),

    PLANNING_EVENT("agent规划");

    private final String name;

    AgentEventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
