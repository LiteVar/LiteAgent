package com.litevar.agent.rest.util;

import cn.hutool.core.convert.Convert;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.util.RedisUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 会话信息保存到redis
 *
 * @author uncle
 * @since 2024/7/12 11:18
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = Convert.toStr(RedisUtil.getValue(String.format(CacheKey.SESSION_CHAT, memoryId.toString())));
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        RedisUtil.setValue(String.format(CacheKey.SESSION_CHAT, memoryId.toString()), json, 1L, TimeUnit.HOURS);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        RedisUtil.delKey(memoryId.toString());
    }
}
