package com.litevar.agent.openai.completion.message;

import com.litevar.agent.openai.completion.Role;
import lombok.Data;

/**
 * 用户消息
 *
 * @author uncle
 * @since 2025/2/13 12:20
 */
@Data
public class UserMessage implements Message {
    private final Role role = Role.USER;
    private Object content;
    private String name;

    @Override
    public Role getRole() {
        return role;
    }

    public static UserMessage of(String content) {
        UserMessage msg = new UserMessage();
        msg.setContent(content);
        return msg;
    }
}
