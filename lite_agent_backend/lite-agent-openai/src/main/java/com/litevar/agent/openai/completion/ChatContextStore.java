package com.litevar.agent.openai.completion;

import com.litevar.agent.openai.completion.message.Message;

import java.util.List;

/**
 * 会话上下文存储
 *
 * @author uncle
 * @since 2025/2/26 11:29
 */
public interface ChatContextStore {
    List<Message> getMessage(String sessionId);

    void updateMessage(String sessionId, List<Message> messages);

    void deleteMessage(String sessionId);
}