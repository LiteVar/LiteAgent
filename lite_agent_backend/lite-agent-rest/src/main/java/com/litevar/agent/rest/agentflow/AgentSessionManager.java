package com.litevar.agent.rest.agentflow;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.event.ModelStatusChangeEvent;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.agentflow.bean.SessionInfo;
import com.litevar.agent.rest.executor.StoreMessageExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AgentSessionManager {
    @Resource
    private AgentRuntimeFactory runtimeFactory;

    public static final long sessionExpireTime = 1;

    @EventListener
    public void onModelStatusChange(ModelStatusChangeEvent event) {
        clearSessionFromUnavailableModel(event.getModelId());
    }

    public String initSession(Agent agent, List<String> datasetIds, Integer debugFlag, String userId, Integer callType) {
        //反思agent不支持直接聊天
        if (Objects.equals(agent.getType(), AgentType.REFLECTION.getType())) {
            throw new ServiceException(ServiceExceptionEnum.REFLECT_AGENT_CANNOT_CHAT);
        }
        Map<String, AgentExecutionSpec> allAgentMap = runtimeFactory.create(agent, datasetIds);
        return cacheData(allAgentMap, agent.getId(), debugFlag, userId, callType);
    }

    public AgentExecutionSpec addTmpAgent(Agent agent, String sessionId) {
        Map<String, AgentExecutionSpec> specMap = runtimeFactory.create(agent, Collections.emptyList());
        AgentExecutionSpec spec = specMap.get(agent.getId());
        cacheAgent(sessionId, agent.getId(), spec);
        return spec;
    }

    public AgentExecutionSpec getAgentRuntimeInfo(String sessionId, String agentId) {
        return (AgentExecutionSpec) RedisUtil.getValue(String.format(CacheKey.SESSION_AGENT_RUNTIME, sessionId, agentId));
    }

    public SessionInfo getSessionInfo(String sessionId) {
        SessionInfo sessionInfo = (SessionInfo) RedisUtil.getValue(String.format(CacheKey.SESSION_INFO, sessionId));
        if (sessionInfo == null) {
            throw new ServiceException(ServiceExceptionEnum.INIT_SESSION_FIRST);
        }
        return sessionInfo;
    }

    public void clearSession(String sessionId) {
        log.info(">>>>>>> clear session:{}", sessionId);
        RedisUtil.delKey(String.format(CacheKey.SESSION_INFO, sessionId));

        //delete agent info cache
        String pattern = String.format(CacheKey.SESSION_AGENT_RUNTIME, sessionId, "*");
        Set<String> cacheAgentKey = RedisUtil.keys(pattern);
        cacheAgentKey.forEach(RedisUtil::delKey);

        StoreMessageExecutor.clear(sessionId);
    }

    public void clearSessionFromAgent(String agentId) {
        // agent发布后清除session
        String pattern = String.format(CacheKey.SESSION_INFO, "*");
        Set<String> cacheSessionKey = RedisUtil.keys(pattern);
        cacheSessionKey.forEach(key -> {
            SessionInfo sessionInfo = (SessionInfo) RedisUtil.getValue(key);
            if (sessionInfo != null && sessionInfo.getAgentId().equals(agentId)) {
                clearSession(sessionInfo.getSessionId());
            }
        });
    }

    public void clearSessionFromUnavailableModel(String modelId) {
        // model禁用后清除session
        Set<String> cacheSessionKey = RedisUtil.keys(String.format(CacheKey.SESSION_INFO, "*"));
        cacheSessionKey.forEach(sessionKey -> {
            String sessionId = sessionKey.substring(sessionKey.lastIndexOf(":") + 1);
            Set<String> agentInfoKeys = RedisUtil.keys(String.format(CacheKey.SESSION_AGENT_RUNTIME, sessionId, "*"));
            for (String agentInfoKey : agentInfoKeys) {
                AgentExecutionSpec agentInfo = (AgentExecutionSpec) RedisUtil.getValue(agentInfoKey);
                if (agentInfo != null && StrUtil.equals(agentInfo.getRequest().getLlmModelId(), modelId)) {
                    log.info("modelId:{}被禁用,所在agent:{}对应的session:{}要清掉", modelId, agentInfo.getAgentName(), sessionId);
                    clearSession(sessionId);
                    break;
                }
            }
        });
    }

    private String cacheData(Map<String, AgentExecutionSpec> allAgentMap, String mainAgentId, Integer debugFlag,
                             String userId, Integer callType) {
        String sessionId = IdUtil.getSnowflakeNextIdStr();
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setModel(allAgentMap.get(mainAgentId).getRequest().getModel());
        sessionInfo.setAgentId(mainAgentId);
        sessionInfo.setUserId(userId);
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setDebugFlag(debugFlag);
        sessionInfo.setCallType(callType);
        RedisUtil.setValue(String.format(CacheKey.SESSION_INFO, sessionId), sessionInfo, sessionExpireTime, TimeUnit.HOURS);

        allAgentMap.forEach((agentId, param) -> cacheAgent(sessionId, agentId, param));

        return sessionId;
    }

    public void cacheAgent(String sessionId, String agentId, AgentExecutionSpec param) {
        log.info("[initAgent] sessionId={},contextId={},agentId={},agentName={}\nmodelName={},baseUrl={}\nfunction={}\n",
                sessionId, param.getRequest().getContextId(), agentId, param.getAgentName(),
                param.getRequest().getModel(), param.getRequest().getBaseUrl(),
                JSONUtil.toJsonStr(param.getRequest().getTools()));

        RedisUtil.setValue(String.format(CacheKey.SESSION_AGENT_RUNTIME, sessionId, agentId), param, sessionExpireTime, TimeUnit.HOURS);
    }

}
