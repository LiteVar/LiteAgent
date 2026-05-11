package com.litevar.agent.openai.completion.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.litevar.agent.openai.completion.Role;
import lombok.Data;

import java.util.List;

/**
 * OpenAI兼容请求消息
 * 用于外部请求透传
 *
 * @author uncle
 * @since 2026/2/11 18:05
 */
@Data
public class AgentOSMessage implements Message {
    private Role role;
    private Object content;
    private String name;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    @JsonProperty("tool_calls")
    private List<AssistantMessage.ToolCall> toolCalls;

    @Override
    public Role getRole() {
        return role;
    }
}
