package com.litevar.agent.openai.completion;

import com.litevar.agent.openai.completion.message.Message;

import java.util.List;

/**
 * @author uncle
 * @since 2025/2/26 17:13
 */
public interface ChatContext {
    void add(String id, List<Message> messages);

    List<Message> getMessages(String id);

    void addTaskMessage(String contextId, String taskId, List<Message> messages);

    List<Message> getTaskMessages(String contextId, String taskId);

    void taskDone(String contextId, String taskId);
}