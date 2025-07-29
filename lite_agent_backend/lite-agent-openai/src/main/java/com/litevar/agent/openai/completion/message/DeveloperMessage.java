package com.litevar.agent.openai.completion.message;

import com.litevar.agent.openai.completion.Role;
import lombok.Data;

/**
 * prompt
 *
 * @author uncle
 * @since 2025/2/13 12:07
 */
@Data
public class DeveloperMessage implements Message {
    private final Role role = Role.DEVELOPER;
    private String content;
    private String name;

    @Override
    public Role getRole() {
        return role;
    }

    public static DeveloperMessage of(String content) {
        DeveloperMessage msg = new DeveloperMessage();
        msg.setContent(content);
        return msg;
    }
}
