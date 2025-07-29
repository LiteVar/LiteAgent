package com.litevar.agent.openai.completion.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.litevar.agent.openai.completion.Role;
import lombok.Data;

import java.util.List;

/**
 * LLM返回消息(text,function-calling)
 *
 * @author uncle
 * @since 2025/2/13 12:23
 */
@Data
public class AssistantMessage implements Message {
    private final Role role = Role.ASSISTANT;
    private String content;
    //序列化时忽略该字段,反序列化时不要忽略
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String reasoningContent;
    private String name;
    /**
     * The refusal message by the assistant.
     */
    private String refusal;
    private List<ToolCall> toolCalls;

    @Override
    public Role getRole() {
        return role;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    @Data
    public static class ToolCall {
        /**
         * 当stream=true时,才有这个字段
         */
        private Integer index;
        private String id;
        /**
         * The type of the tool. Currently, only function is supported.
         */
        private String type;
        private Function function;
    }

    @Data
    public static class Function {
        private String name;
        private String arguments;
    }
}
