package com.litevar.agent.core.event;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.AgentChatMessage;
import com.litevar.agent.base.repository.AgentChatMessageRepository;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author uncle
 * @since 2024/9/3 16:24
 */
@Component
public class AgentMessageListener {

    @Autowired
    private AgentChatMessageRepository agentChatMessageRepository;

    @Async
    @EventListener(AgentMessageEvent.class)
    public void onAgentMessageEvent(AgentMessageEvent event) {
        AgentChatMessage.TaskMessage taskMessage = new AgentChatMessage.TaskMessage();

        List<OutMessage> messageList = RedisUtil.getListValue(String.format(CacheKey.TASK_MESSAGE, event.getTaskId()))
                .stream().map(o -> (OutMessage) o).toList();
        if (messageList.isEmpty()) {
            return;
        }
        taskMessage.setMessage(messageList);
        RedisUtil.delKey(String.format(CacheKey.TASK_MESSAGE, event.getTaskId()));

        taskMessage.setTaskId(event.getTaskId());

        AgentChatMessage.TokenUsage usage = (AgentChatMessage.TokenUsage) RedisUtil.getValue(String.format(CacheKey.TOKEN_USAGE, event.getTaskId()));
        taskMessage.setTokenUsage(usage);
        RedisUtil.delKey(String.format(CacheKey.TOKEN_USAGE, event.getTaskId()));

        AgentChatMessage agentChatMessage = agentChatMessageRepository.findBySessionId(event.getSessionId());
        if (agentChatMessage == null) {
            agentChatMessage = new AgentChatMessage();
            JSONObject sessionInfo = (JSONObject) RedisUtil.getValue(String.format(CacheKey.SESSION_INFO, event.getSessionId()));
            BeanUtil.copyProperties(sessionInfo, agentChatMessage);

            agentChatMessage.setTaskMessage(List.of(taskMessage));

            agentChatMessageRepository.insert(agentChatMessage);

        } else {
            List<AgentChatMessage.TaskMessage> taskMessageList = agentChatMessage.getTaskMessage();
            if (taskMessageList == null) {
                taskMessageList = new ArrayList<>();
            }
            taskMessageList.add(taskMessage);
            agentChatMessage.setTaskMessage(taskMessageList);
            agentChatMessageRepository.save(agentChatMessage);
        }
    }
}
