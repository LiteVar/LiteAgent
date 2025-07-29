package com.litevar.agent.rest.openai.executor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.AgentChatMessage;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import com.mongoplus.aggregate.AggregateWrapper;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.conditions.update.UpdateWrapper;
import com.mongoplus.mapper.BaseMapper;
import com.mongoplus.toolkit.FunctionUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 消息持久化executor
 *
 * @author uncle
 * @since 2025/3/19 15:55
 */
@Component
public class StoreMessageExecutor {
    private static StoreMessageExecutor executor;
    private static final Map<String, SessionContext> sessionMap = new ConcurrentHashMap<>();

    @Autowired
    private BaseMapper baseMapper;

    @PostConstruct
    public void init() {
        executor = getInstance();
        executor.baseMapper = baseMapper;
    }

    public static void store(String sessionId, List<OutMessage> messages) {
        sessionMap.computeIfAbsent(sessionId, k ->
                        new SessionContext(new ConcurrentLinkedQueue<>(), new AtomicInteger(0)))
                .messageQueue.addAll(messages);
        CompletableFuture.runAsync(() -> getInstance().store(sessionId));
    }

    public static void clear(String sessionId) {
        SessionContext context = sessionMap.get(sessionId);
        if (context != null) {
            if (context.messageQueue.isEmpty()) {
                sessionMap.remove(sessionId);
            } else {
                //有任务未执行,延迟清空
                CompletableFuture.delayedExecutor(20, TimeUnit.SECONDS).execute(() -> sessionMap.remove(sessionId));
            }
        }
    }

    public void store(String sessionId) {
        SessionContext sessionContext = sessionMap.get(sessionId);
        if (!sessionContext.lack.compareAndSet(0, 1)) {
            return;
        }
        Queue<OutMessage> queue = sessionContext.messageQueue;
        List<OutMessage> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            OutMessage element = queue.poll();
            if (element != null) {
                list.add(element);
            }
        }
        if (list.isEmpty()) {
            sessionContext.lack.set(0);
            return;
        }

        Map<String, List<OutMessage>> taskMessageMap = list.stream().collect(Collectors.groupingBy(OutMessage::getTaskId));
        List<String> existTaskIds = queryExistTaskId(sessionId);

        if (ObjectUtil.isEmpty(existTaskIds)) {
            AgentChatMessage agentMessage = new AgentChatMessage();
            JSONObject sessionInfo = (JSONObject) RedisUtil.getValue(String.format(CacheKey.SESSION_INFO, sessionId));
            BeanUtil.copyProperties(sessionInfo, agentMessage);

            List<AgentChatMessage.TaskMessage> taskMessages = new ArrayList<>();
            taskMessageMap.forEach((taskId, messageList) -> {
                AgentChatMessage.TaskMessage taskMessage = new AgentChatMessage.TaskMessage();
                taskMessage.setTaskId(taskId);
                taskMessage.setMessage(messageList);
                taskMessages.add(taskMessage);
            });
            agentMessage.setTaskMessage(taskMessages);
            baseMapper.save(agentMessage);
        } else {
            for (Map.Entry<String, List<OutMessage>> entry : taskMessageMap.entrySet()) {
                String taskId = entry.getKey();
                List<OutMessage> messageList = entry.getValue();
                //taskId存在,则往message数组里追加数据;
                if (existTaskIds.contains(taskId)) {
                    baseMapper.update(new UpdateWrapper<AgentChatMessage>()
                            .push("task_message.$.message", messageList)
                            .eq("session_id", sessionId)
                            .eq("task_message.task_id", taskId), AgentChatMessage.class);
                } else {
                    // 不存在,则新加一条taskMessage数据到taskMessage数组中
                    AgentChatMessage.TaskMessage taskMessage = new AgentChatMessage.TaskMessage();
                    taskMessage.setTaskId(taskId);
                    taskMessage.setMessage(messageList);
                    baseMapper.update(new UpdateWrapper<AgentChatMessage>()
                            .push(AgentChatMessage::getTaskMessage, taskMessage)
                            .eq(AgentChatMessage::getSessionId, sessionId), AgentChatMessage.class);
                }
            }
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        sessionContext.lack.set(0);
        store(sessionId);
    }

    private List<String> queryExistTaskId(String sessionId) {
        AggregateWrapper wrapper = new AggregateWrapper();
        //只处理sessionId的数据
        wrapper.match(new QueryWrapper<AgentChatMessage>().eq(AgentChatMessage::getSessionId, sessionId));

        //展开task_message
        wrapper.unwind(FunctionUtil.getFieldNameOption(AgentChatMessage::getTaskMessage));

        String taskIdName = FunctionUtil.builderFunction().add(AgentChatMessage::getTaskMessage).add(AgentChatMessage.TaskMessage::getTaskId).build(true);
        //提取task_id字段
        wrapper.group(taskIdName);

        return baseMapper.aggregateList(wrapper, AgentChatMessage.class)
                .stream().map(AgentChatMessage::getId).toList();
    }

    private static StoreMessageExecutor getInstance() {
        if (executor == null) {
            executor = new StoreMessageExecutor();
        }
        return executor;
    }

    private record SessionContext(Queue<OutMessage> messageQueue, AtomicInteger lack) {
    }
}
