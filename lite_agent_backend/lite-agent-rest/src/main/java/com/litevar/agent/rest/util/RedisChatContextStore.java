package com.litevar.agent.rest.util;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.openai.completion.ChatContextStore;
import com.litevar.agent.openai.completion.message.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author uncle
 * @since 2025/2/26 15:46
 */
@Component
public class RedisChatContextStore implements ChatContextStore {

    @Override
    public List<Message> getMessage(String sessionId) {
        Object value = RedisUtil.getValue(String.format(CacheKey.SESSION_CHAT, sessionId));
        if (value == null) {
            return new ArrayList<>();
        }
        return (List<Message>) value;
    }

    @Override
    public void updateMessage(String sessionId, List<Message> messages) {
        RedisUtil.setValue(String.format(CacheKey.SESSION_CHAT, sessionId), messages, 1L, TimeUnit.HOURS);
    }

    @Override
    public void deleteMessage(String sessionId) {
        RedisUtil.delKey(String.format(CacheKey.SESSION_CHAT, sessionId));
    }
}
