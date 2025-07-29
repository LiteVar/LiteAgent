package com.litevar.agent.rest.openai.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ReflectMessageInfo;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.openai.executor.StoreMessageExecutor;
import com.litevar.agent.rest.openai.executor.TaskExecutor;
import com.litevar.agent.rest.openai.handler.AgentMessageHandler;
import com.litevar.agent.rest.openai.handler.LogMessageHandler;
import com.litevar.agent.rest.openai.handler.ResponseMessageHandler;
import com.litevar.agent.rest.openai.handler.StoreMessageHandler;
import com.litevar.agent.rest.openai.message.AgentMessage;
import com.litevar.agent.rest.util.AgentUtil;
import com.litevar.agent.rest.util.SpringUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author uncle
 * @since 2025/3/5 14:34
 */
@Slf4j
public class AgentManager {
    private static final Map<String, AgentSession> sessionMap = new ConcurrentHashMap<>();

    public static String initSession(Agent agent, List<String> datasetIds, Integer debugFlag, String userId, Integer callType) {
        String sessionId = IdUtil.getSnowflakeNextIdStr();
        MultiAgent agentInstance = SpringUtil.getBean(AgentUtil.class).buildAgent(agent, datasetIds, sessionId);

        AgentSession session = new AgentSession();
        session.setAgent(agentInstance);

        //日志
        session.getHandlers().add(new LogMessageHandler(sessionId));
        //数据持久化
        session.getHandlers().add(new StoreMessageHandler(sessionId));

        sessionMap.put(sessionId, session);

        cacheSessionData(sessionId, agentInstance, debugFlag, userId, callType);

        return sessionId;
    }

    public static Map<String, CompletionResponse> chat(MultiAgent agent, String taskId, List<Message> submitMsg, boolean stream) {
        Callable<Object> task = () -> {
            if (ObjectUtil.isNotEmpty(agent.getReflectAgentMap())) {
                //有反思agent,保存本次会话数据
                ReflectMessageInfo reflect = new ReflectMessageInfo();
                reflect.setInput(((UserMessage) submitMsg.get(0)).getContent().toString());
                RedisUtil.setValue(String.format(CacheKey.REFLECT_INFO, taskId), reflect, 10, TimeUnit.MINUTES);
            }
            ResponseMessageHandler handler = new ResponseMessageHandler(agent.getAgentId(), taskId);
            List<AgentMessageHandler> handlerList = getHandler(agent.getSessionId());
            handlerList.add(handler);
            agent.generate(taskId, submitMsg, stream);
            while (!StrUtil.equals(handler.getStatus(), ResponseMessageHandler.STATE_COMPLETED)) {
                //阻塞,不要让任务结束
                Thread.sleep(500);
            }
            handlerList.remove(handler);

            return handler.getResponseMap();
        };

        try {
            //id使用sessionId表示执行模式控制范围是在同一个会话中,initSession时,每个agent都会生成contextId,相当于子sessionId
            CompletableFuture<Object> future = TaskExecutor.execute(agent.getContextId(), agent.getExecuteMode(), task);
            return (Map<String, CompletionResponse>) future.join();
        } catch (Exception e) {
            //reject模式:不能执行时直接以error返回
            agent.getCallback().onError(taskId, e);
            return null;
        }
    }

    public static MultiAgent getAgent(String sessionId) {
        AgentSession session = getSession(sessionId);
        return session.getAgent();
    }

    private static AgentSession getSession(String sessionId) {
        AgentSession session = sessionMap.get(sessionId);
        if (session == null || session.getAgent() == null) {
            throw new ServiceException(ServiceExceptionEnum.INIT_SESSION_FIRST);
        }
        return session;
    }

    public static List<AgentMessageHandler> getHandler(String sessionId) {
        AgentSession session = getSession(sessionId);
        return session.getHandlers();
    }

    @PostConstruct
    public void init() {
        Runnable task = () -> sessionMap.forEach((sessionId, value) -> {
            boolean flag = RedisUtil.exists(String.format(CacheKey.SESSION_INFO, sessionId));
            if (flag) {
                clearSession(sessionId);
            }
        });
        Executors.newScheduledThreadPool(5)
                .scheduleWithFixedDelay(task, 3, 2, TimeUnit.MINUTES);
    }

    public static void clearSession(String sessionId) {
        AgentSession session = sessionMap.get(sessionId);
        if (session == null) return;

        MultiAgent agent = session.getAgent();
        if (ObjectUtil.isNotEmpty(agent.getReflectAgentMap())) {
            agent.getReflectAgentMap().clear();
        }
        if (ObjectUtil.isNotEmpty(agent.getDistributeAgentMap())) {
            agent.getDistributeAgentMap().clear();
        }
        if (ObjectUtil.isNotEmpty(agent.getGeneralAgentMap())) {
            agent.getGeneralAgentMap().clear();
        }
        session.getHandlers().clear();
        sessionMap.remove(sessionId);
        TaskExecutor.clear(sessionId);
        StoreMessageExecutor.clear(sessionId);
    }

    public static void cacheSessionData(String sessionId, MultiAgent agentInstance, Integer debugFlag, String userId, Integer callType) {
        JSONObject obj = new JSONObject();
        obj.set("model", agentInstance.getRequest().getModel());
        obj.set("agentId", agentInstance.getAgentId());
        obj.set("userId", userId);
        obj.set("sessionId", sessionId);
        obj.set("debugFlag", debugFlag);
        obj.set("callType", callType);
        RedisUtil.setValue(String.format(CacheKey.SESSION_INFO, sessionId), obj, 1L, TimeUnit.HOURS);
    }

    public static void handleMsg(AgentMsgType msgType, AgentMessage msg) {
        msgType.handler(msg);
    }
}
