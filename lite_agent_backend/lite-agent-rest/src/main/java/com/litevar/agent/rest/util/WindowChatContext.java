package com.litevar.agent.rest.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.openai.completion.ChatContext;
import com.litevar.agent.openai.completion.ChatContextStore;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.DeveloperMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.ToolMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author uncle
 * @since 2025/2/26 11:34
 */
@Component
public class WindowChatContext implements ChatContext {
    private final int MAX_MESSAGES = 20;
    @Resource
    private ChatContextStore store;

    @Override
    public void add(String id, List<Message> messages) {
        List<Message> allMessage = getMessages(id);
        Optional<DeveloperMessage> originDevMsg = allMessage.stream().filter(msg -> msg instanceof DeveloperMessage)
                .map(msg -> (DeveloperMessage) msg)
                .findAny();
        if (originDevMsg.isPresent()) {
            Optional<Message> dev = messages.stream().filter(msg -> msg instanceof DeveloperMessage).findFirst();
            if (dev.isPresent()) {
                if (StrUtil.equals(originDevMsg.get().getContent(),
                        ((DeveloperMessage) dev.get()).getContent())) {
                    return;
                } else {
                    //只能有一条system message
                    allMessage.remove(originDevMsg.get());
                }
            }
        }
        allMessage.addAll(messages);
        ensureCapacity(allMessage);
        store.updateMessage(id, allMessage);
    }

    @Override
    public List<Message> getMessages(String id) {
        return store.getMessage(id);
    }

    public void ensureCapacity(List<Message> messages) {
        while (messages.size() > MAX_MESSAGES) {
            int messageToEvictIndex = 0;
            if (messages.get(0) instanceof DeveloperMessage) {
                messageToEvictIndex = 1;
            }
            Message evictedMessage = messages.remove(messageToEvictIndex);
            if (evictedMessage instanceof AssistantMessage &&
                    ((AssistantMessage) evictedMessage).hasToolCalls()) {
                //function-calling消息,需要将这条消息所对应的ToolMessage也删掉
                List<String> toolCallIds = ((AssistantMessage) evictedMessage).getToolCalls()
                        .parallelStream().map(AssistantMessage.ToolCall::getId).toList();
                messages.removeIf(m -> m instanceof ToolMessage && toolCallIds.contains(((ToolMessage) m).getToolCallId()));
            }
        }
    }

    /*** 以下为task message 相关操作 **/

    @Override
    public void addTaskMessage(String contextId, String taskId, List<Message> messages) {
        List<Message> list = getTaskMessages(contextId, taskId);
        list.addAll(messages);
        store.updateMessage(contextId + "-" + taskId, list);
    }

    @Override
    public List<Message> getTaskMessages(String contextId, String taskId) {
        return store.getMessage(contextId + "-" + taskId);
    }

    @Override
    public void taskDone(String contextId, String taskId) {
        //任务完成,把task上下文加入总的上下文
        List<Message> list = getTaskMessages(contextId, taskId);
        if (ObjectUtil.isNotEmpty(list)) {
            add(contextId, list);
            store.deleteMessage(contextId + "-" + taskId);
        }
    }
}