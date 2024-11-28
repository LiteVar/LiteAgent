package com.litevar.agent.core.module.agent;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.AgentDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.AgentChatMessageRepository;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.AgentSessionVO;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author uncle
 * @since 2024/9/9 16:39
 */
@Service
public class ChatService {

    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentChatMessageRepository agentChatMessageRepository;
    @Autowired
    private LocalAgentService localAgentService;

    public List<AgentSessionVO> recentAgent(String workspaceId) {
        List<AgentDTO> list = agentService.agentList(workspaceId, 0, null, 1);
        List<AgentDTO> localList = agentService.agentList(workspaceId, 4, null, 1);
        list.addAll(localList);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> agentIds = list.stream().map(AgentDTO::getId).toList();

        QAgentChatMessage qAgentChatMessage = QAgentChatMessage.agentChatMessage;
        BooleanExpression expression = qAgentChatMessage.userId.eq(LoginContext.currentUserId())
                .and(qAgentChatMessage.agentId.in(agentIds))
                .and(qAgentChatMessage.debugFlag.eq(0))
                .and(qAgentChatMessage.deleted.isFalse());

        Iterable<AgentChatMessage> allChat = agentChatMessageRepository.findAll(expression);
        Map<String, AgentChatMessage> map = StreamSupport.stream(allChat.spliterator(), false)
                .collect(Collectors.toMap(AgentChatMessage::getAgentId,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(AgentChatMessage::getCreateTime))));
        List<AgentSessionVO> res = map.entrySet().stream().map(i -> {
                    AgentSessionVO vo = new AgentSessionVO();
                    vo.setAgentId(i.getKey());
                    vo.setCreateTime(i.getValue().getCreateTime());
                    return vo;
                })
                .sorted(Comparator.comparing(AgentSessionVO::getCreateTime).reversed())
                .toList();

        if (!res.isEmpty()) {
            Set<String> ids = res.stream().map(AgentSessionVO::getAgentId).collect(Collectors.toSet());
            Map<String, String> agentMap = agentService.findByIds(ids).stream().collect(Collectors.toMap(Agent::getId, Agent::getName));
            Map<String, String> localAgentMap = localAgentService.agentByIds(ids).stream().collect(Collectors.toMap(Agent::getId, Agent::getName));

            res.forEach(i -> {
                String name = agentMap.get(i.getAgentId());
                if (StrUtil.isNotEmpty(name)) {
                    i.setLocalFlag(false);
                } else {
                    name = localAgentMap.get(i.getAgentId());
                    i.setLocalFlag(true);
                }
                i.setName(name);
            });
        }

        return res;
    }

    public List<AgentChatMessage.TaskMessage> agentChat(String agentId, Integer debugFlag) {
        QAgentChatMessage qAgentChatMessage = QAgentChatMessage.agentChatMessage;
        BooleanExpression expression = qAgentChatMessage.userId.eq(LoginContext.currentUserId())
                .and(qAgentChatMessage.agentId.eq(agentId))
                .and(qAgentChatMessage.debugFlag.eq(debugFlag)
                        .and(qAgentChatMessage.deleted.isFalse()));
        Iterable<AgentChatMessage> messages = agentChatMessageRepository.findAll(expression, Sort.by(Sort.Direction.DESC, "createTime"));
        return StreamSupport.stream(messages.spliterator(), false)
                .map(i -> {
                    List<AgentChatMessage.TaskMessage> list = i.getTaskMessage();
                    Collections.reverse(list);
                    return list;
                })
                .flatMap(Collection::stream)
                .toList();
    }

    public void checkChatData(Agent agent, LlmModel model, List<ToolProvider> toolList) {
        // 如果agent创建者分享了agent,其内的model,tool只要是创建者创建的,无需分享,别人就可以使用;
        // 如果使用了别人的model,tool,需要别人打开分享才可以使用
        String userId = LoginContext.currentUserId();
        if (agent == null || (!StrUtil.equals(userId, agent.getUserId()) && !agent.getShareFlag())) {
            throw new ServiceException(ServiceExceptionEnum.AGENT_NOT_EXIST_OR_NOT_SHARE);
        }

        if (model == null) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }

        if (ObjectUtil.isNotEmpty(toolList)) {
            List<String> error = new ArrayList<>();
            toolList.forEach(tool -> {
                if (!StrUtil.equals(agent.getUserId(), tool.getUserId()) && !tool.getShareFlag()) {
                    error.add("工具" + tool.getName() + "已取消分享");
                }
            });
            if (!error.isEmpty()) {
                throw new ServiceException(30003, StrUtil.join(",", error));
            }
        }

        if (ObjectUtil.isNotEmpty(agent.getMaxTokens())) {
            Integer modelMaxTokens = ObjectUtil.isNotEmpty(model.getMaxTokens()) ? model.getMaxTokens() : 4096;
            if (agent.getMaxTokens() > modelMaxTokens) {
                throw new ServiceException(ServiceExceptionEnum.MAX_TOKEN_LARGER);
            }
        }
    }

    public void cacheChatToolData(String sessionId, List<ToolProvider> toolList, List<ToolFunction> functionList) {
        if (ObjectUtil.isNotEmpty(toolList)) {
            toolList.forEach(tool -> {
                if (StrUtil.isNotEmpty(tool.getApiKeyType()) && StrUtil.isNotEmpty(tool.getApiKey())) {
                    String key = tool.getApiKeyType() + " " + tool.getApiKey();
                    RedisUtil.setValue(String.format(CacheKey.TOOL_API_KEY, sessionId, tool.getId()), key, 1L, TimeUnit.HOURS);
                }
            });
        }
        if (ObjectUtil.isNotEmpty(functionList)) {
            functionList.forEach(function -> RedisUtil.setValue(String.format(CacheKey.SESSION_FUNCTION_INFO, sessionId,
                    function.getId()), function, 1L, TimeUnit.HOURS));
        }
    }

    /**
     * 清空调试记录
     */
    public void clearChatData(String agentId) {
        String userId = LoginContext.currentUserId();
        QAgentChatMessage qAgentChatMessage = QAgentChatMessage.agentChatMessage;
        BooleanExpression expression = qAgentChatMessage.userId.eq(userId)
                .and(qAgentChatMessage.agentId.eq(agentId))
                .and(qAgentChatMessage.debugFlag.eq(1));
        Iterable<AgentChatMessage> list = agentChatMessageRepository.findAll(expression);
        list.forEach(chat -> chat.setDeleted(Boolean.TRUE));
        agentChatMessageRepository.saveAll(list);
    }
}