package com.litevar.agent.rest.agentflow;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.openai.ObjectMapperSingleton;
import com.litevar.agent.openai.completion.ChatContext;
import com.litevar.agent.openai.completion.ChatContextStore;
import com.litevar.agent.openai.completion.message.DeveloperMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.tool.ToolSpecification;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.agentflow.bean.SessionInfo;
import com.litevar.agent.rest.util.MessageTokenUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Task queue based chat context merge and summarization.
 *
 * @author uncle
 * @since 2026/01/23 15:03
 */
@Slf4j
@Component
public class TaskQueueChatContext implements ChatContext {
    private static final double SUMMARY_THRESHOLD = 0.9d;
    private static final long LOCK_TTL_SECONDS = 60;

    @Resource
    private ChatContextStore store;
    @Resource
    private ModelService modelService;
    @Resource
    private AgentSessionManager manager;

    private final ContextCompactor contextCompress = new ContextCompactor();

    @Override
    public void add(String id, List<Message> messages) {
        List<Message> allMessage = getMessages(id);
        DeveloperMessage originDevMsg = null;
        for (Message msg : allMessage) {
            if (msg instanceof DeveloperMessage) {
                originDevMsg = (DeveloperMessage) msg;
                break;
            }
        }
        if (originDevMsg != null) {
            for (Message msg : messages) {
                if (msg instanceof DeveloperMessage dev) {
                    if (StrUtil.equals(originDevMsg.getContent(), dev.getContent())) {
                        return;
                    }
                    allMessage.remove(originDevMsg);
                    break;
                }
            }
        }
        allMessage.addAll(messages);
        store.updateMessage(id, allMessage);
    }

    @Override
    public List<Message> getMessages(String id) {
        return store.getMessage(id);
    }

    /*** 以下为task message 相关操作 **/

    @Override
    public void addTaskMessage(String contextId, String taskId, List<Message> messages) {
        List<Message> list = getTaskMessages(contextId, taskId);
        list.addAll(messages);
        updateTaskMessage(contextId, taskId, list);
    }

    private void updateTaskMessage(String contextId, String taskId, List<Message> messages) {
        store.updateMessage(contextId + "-" + taskId, messages);
    }

    @Override
    public List<Message> getTaskMessages(String contextId, String taskId) {
        return store.getMessage(contextId + "-" + taskId);
    }

    @Override
    public void taskDone(String sessionId, String agentId, String taskId) {
        AgentExecutionSpec runtimeInfo = manager.getAgentRuntimeInfo(sessionId, agentId);
        SessionInfo sessionInfo = manager.getSessionInfo(sessionId);
        String modelId = runtimeInfo.getRequest().getLlmModelId();
        String contextId = runtimeInfo.getRequest().getContextId();
        Integer turns = runtimeInfo.getRequest().getTurns();

        //任务完成,把task上下文加入总的上下文
        //先加入队列统一处理,以防并发时出现数据覆盖
        String queueKey = String.format(CacheKey.SESSION_CHAT_TASK_QUEUE, contextId);
        RedisUtil.rightPush(queueKey, taskId);
        RedisUtil.expire(queueKey, AgentSessionManager.sessionExpireTime, TimeUnit.HOURS);

        String lockKey = String.format(CacheKey.SESSION_CHAT_TASK_QUEUE_LOCK, contextId);
        if (!Boolean.TRUE.equals(RedisUtil.setNx(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS))) {
            return;
        }
        try {
            LlmModel model = null;
            if (StrUtil.isNotEmpty(modelId)) {
                //todo local model
                model = modelService.findById(modelId);
            }
            String toolSchema = "";
            try {
                List<ToolSpecification> tools = runtimeInfo.getRequest().getTools();
                if (ObjectUtil.isNotEmpty(tools)) {
                    toolSchema = ObjectMapperSingleton.getObjectMapper().writeValueAsString(tools);
                }
            } catch (JsonProcessingException ignore) {
            }
            while (true) {
                //队列中待合并的task上下文
                List<String> mergedTaskIds = new ArrayList<>();
                List<Message> allMessage = new ArrayList<>(getMessages(contextId));
                while (true) {
                    Object value = RedisUtil.leftPop(queueKey);
                    if (value == null) {
                        break;
                    }
                    String queuedTaskId = String.valueOf(value);
                    mergedTaskIds.add(queuedTaskId);
                    List<Message> taskMessages = getTaskMessages(contextId, queuedTaskId);
                    if (ObjectUtil.isNotEmpty(taskMessages)) {
                        allMessage.addAll(taskMessages);
                    }
                }
                if (mergedTaskIds.isEmpty()) {
                    return;
                }

                List<Message> toStore = allMessage;
                if (model != null && shouldSummarize(allMessage, toolSchema, model.getContextWindows())) {
                    log.info("compress context, contextId:{},turns:{}", contextId, turns);
                    TokenReportDTO tokenReport = new TokenReportDTO(sessionInfo.getUserId(), model.getId(), agentId, sessionId);
                    toStore = contextCompress.compress(allMessage, turns, model, tokenReport);
                }
                try {
                    store.updateMessage(contextId, toStore);
                    for (String mergedId : mergedTaskIds) {
                        store.deleteMessage(contextId + "-" + mergedId);
                    }
                } catch (Exception e) {
                    log.error("merge context failed, contextId:{}, taskIds:{}", contextId, mergedTaskIds, e);
                    for (int i = mergedTaskIds.size() - 1; i >= 0; i--) {
                        RedisUtil.leftPush(queueKey, mergedTaskIds.get(i));
                    }
                    RedisUtil.expire(queueKey, AgentSessionManager.sessionExpireTime, TimeUnit.HOURS);
                    return;
                }
            }
        } finally {
            RedisUtil.delKey(lockKey);
        }
    }

    @Override
    public boolean compressTask(String sessionId, String agentId, String taskId) {
        SessionInfo sessionInfo = manager.getSessionInfo(sessionId);
        AgentExecutionSpec runtimeInfo = manager.getAgentRuntimeInfo(sessionId, agentId);
        String contextId = runtimeInfo.getRequest().getContextId();
        String modelId = runtimeInfo.getRequest().getLlmModelId();
        LlmModel model = modelService.findById(modelId);
        TokenReportDTO tokenReport = new TokenReportDTO(sessionInfo.getUserId(), model.getId(), agentId, sessionId);
        List<Message> taskMessages = getTaskMessages(contextId, taskId);
        List<Message> result = contextCompress.compressTask(taskMessages, model, tokenReport);
        if (result != null) {
            updateTaskMessage(contextId, taskId, result);
            return true;
        }
        return false;
    }

    private boolean shouldSummarize(List<Message> messages, String toolSchema, Integer contextWindows) {
        if (contextWindows == null || contextWindows <= 0) {
            //没配置,默认为64k
            contextWindows = 64_000;
        }
        int totalTokens = MessageTokenUtil.countTokens(messages) + MessageTokenUtil.countTokens(toolSchema);
        //总token大于maxToken的90%时,需要进行compress
        int threshold = (int) Math.ceil(contextWindows * SUMMARY_THRESHOLD);
        return totalTokens >= threshold;
    }
}
