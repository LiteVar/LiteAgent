package com.litevar.agent.openai.completion.message;

import com.litevar.agent.openai.completion.Role;

/**
 * @author uncle
 * @since 2025/2/13 12:07
 */
public interface Message {
    Role getRole();
}