package com.litevar.agent.openai.completion.message;

import com.litevar.agent.openai.completion.Role;
import lombok.Data;

/**
 * 工具结果返回消息
 *
 * @author uncle
 * @since 2025/2/13 12:24
 */
@Data
public class ToolMessage implements Message {
    private final Role role = Role.TOOL;
    private String content;
    private String toolCallId;

    @Override
    public Role getRole() {
        return role;
    }
}
