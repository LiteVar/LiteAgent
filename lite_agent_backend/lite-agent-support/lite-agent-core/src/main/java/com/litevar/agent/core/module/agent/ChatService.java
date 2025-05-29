package com.litevar.agent.core.module.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.dto.AgentDTO;
import com.litevar.agent.base.dto.MessageDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.AgentChatMessage;
import com.litevar.agent.base.enums.AgentCallType;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.AgentSessionVO;
import com.litevar.agent.base.vo.ExternalMessage;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.conditions.update.LambdaUpdateChainWrapper;
import com.mongoplus.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2024/9/9 16:39
 */
@Slf4j
@Service
public class ChatService {

    @Autowired
    private AgentService agentService;
    @Autowired
    private LocalAgentService localAgentService;
    @Autowired
    private BaseMapper baseMapper;

    public List<AgentSessionVO> recentAgent(String workspaceId) {
        List<AgentDTO> list = agentService.agentList(workspaceId, 0, null, 1);
        List<AgentDTO> localList = agentService.agentList(workspaceId, 4, null, 1);
        list.addAll(localList);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> agentIds = list.stream().map(AgentDTO::getId).toList();

        List<AgentChatMessage> allChat = baseMapper.list(new QueryWrapper<AgentChatMessage>().lambdaQuery()
                .projectDisplay(AgentChatMessage::getAgentId, AgentChatMessage::getCreateTime)
                .eq(AgentChatMessage::getUserId, LoginContext.currentUserId())
                .in(AgentChatMessage::getAgentId, agentIds)
                .eq(AgentChatMessage::getDebugFlag, 0), AgentChatMessage.class);

        Map<String, AgentChatMessage> map = allChat.stream()
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
            Map<String, String> agentMap = agentService.getByIds(ids).stream().collect(Collectors.toMap(Agent::getId, Agent::getName));
            Map<String, String> localAgentMap = localAgentService.getByIds(ids).stream().collect(Collectors.toMap(Agent::getId, Agent::getName));

            res.forEach(i -> {
                String name = agentMap.get(i.getAgentId());
                i.setLocalFlag(StrUtil.isEmpty(name));
                i.setName(StrUtil.emptyToDefault(name, localAgentMap.get(i.getAgentId())));
            });
        }

        return res;
    }

    public List<MessageDTO> agentChat(String agentId, Integer debugFlag) {
        List<AgentChatMessage> messages = baseMapper.list(new QueryWrapper<AgentChatMessage>().lambdaQuery()
                .projectDisplay(AgentChatMessage::getTaskMessage, AgentChatMessage::getSessionId)
                .eq(AgentChatMessage::getUserId, LoginContext.currentUserId())
                .eq(AgentChatMessage::getAgentId, agentId)
                .eq(AgentChatMessage::getDebugFlag, debugFlag)
                .orderByDesc(AgentChatMessage::getCreateTime), AgentChatMessage.class);

        return messages.stream()
                .map(i -> {
                    List<MessageDTO> list = new ArrayList<>();
                    i.getTaskMessage().forEach(t -> {
                        MessageDTO dto = new MessageDTO();
                        dto.setSessionId(i.getSessionId());
                        dto.setTaskId(t.getTaskId());
                        //todo 临时解决mongo-plus Object类型下划线转换问题
                        for (OutMessage outMessage : t.getMessage()) {
                            if (StrUtil.equals(outMessage.getType(), "knowledge")) {
                                String str = JSONUtil.toJsonStr(outMessage.getContent());
                                OutMessage.KnowledgeContent knowledgeContent = JSONUtil.toBean(str, OutMessage.KnowledgeContent.class);
                                outMessage.setContent(knowledgeContent);
                            } else if (StrUtil.equals(outMessage.getType(), "dispatch")) {
                                String str = JSONUtil.toJsonStr(outMessage.getContent());
                                OutMessage.DistributeContent content = JSONUtil.toBean(str, OutMessage.DistributeContent.class);
                                outMessage.setContent(content);
                            } else if (StrUtil.equals(outMessage.getType(), "agentSwitch")) {
                                String str = JSONUtil.toJsonStr(outMessage.getContent());
                                OutMessage.AgentSwitchContent content = JSONUtil.toBean(str, OutMessage.AgentSwitchContent.class);
                                outMessage.setContent(content);
                            }
                        }
                        dto.setMessage(t.getMessage());
                        list.add(dto);
                    });
                    Collections.reverse(list);
                    return list;
                })
                .flatMap(Collection::stream)
                .toList();
    }

    public List<ExternalMessage> agentSessionChat(String agentId, String sessionId) {
        AgentChatMessage agentChatMessage = baseMapper.one(new QueryWrapper<AgentChatMessage>().lambdaQuery()
                .projectDisplay(AgentChatMessage::getTaskMessage, AgentChatMessage::getSessionId)
                .eq(AgentChatMessage::getAgentId, agentId)
                .eq(AgentChatMessage::getCallType, AgentCallType.EXTERNAL.getCallType())
                .eq(AgentChatMessage::getSessionId, sessionId), AgentChatMessage.class);
        List<ExternalMessage> resList = new ArrayList<>();
        if (agentChatMessage != null) {
            agentChatMessage.getTaskMessage().forEach(i -> {
                for (OutMessage outMessage : i.getMessage()) {
                    ExternalMessage message = new ExternalMessage();
                    message.setSessionId(agentChatMessage.getSessionId());
                    message.setTaskId(i.getTaskId());
                    if (StrUtil.equals(outMessage.getRole(), "user") && StrUtil.equals(outMessage.getType(), "text")) {
                        //用户发送的文本
                        message.setRole("user");
                        message.setTo("agent");
                        message.setType("contentList");
                        message.setContent(List.of(Dict.create().set("type", "text").set("message", outMessage.getContent())));

                    } else if (StrUtil.equals(outMessage.getRole(), "assistant") && StrUtil.equals(outMessage.getType(), "text")) {
                        //大模型回复的文本
                        message.setRole("assistant");
                        message.setTo("agent");
                        message.setType("text");
                        message.setContent(outMessage.getContent());

                    } else if (StrUtil.equals(outMessage.getRole(), "assistant") && StrUtil.equals(outMessage.getType(), "functionCallList")) {
                        //大模型function-calling消息
                        message.setRole("assistant");
                        message.setTo("agent");
                        message.setType("toolCalls");
                        message.setContent(outMessage.getToolCalls());

                    } else if (StrUtil.equals(outMessage.getRole(), "tool") && StrUtil.equals(outMessage.getType(), "toolReturn")) {
                        //工具返回消息
                        message.setRole("tool");
                        message.setTo("agent");
                        message.setType("toolReturn");
                        ExternalMessage.ToolReturn toolReturn = new ExternalMessage.ToolReturn();
                        toolReturn.setId(outMessage.getToolCallId());
                        toolReturn.setResult(outMessage.getContent().toString());
                        message.setContent(toolReturn);

                    } else if (StrUtil.equals(outMessage.getRole(), "agent") && StrUtil.equals(outMessage.getType(), "dispatch")) {
                        //agent分发
                        message.setRole("subagent");
                        message.setTo("agent");
                        message.setType("dispatch");

                        String str = JSONUtil.toJsonStr(outMessage.getContent());
                        OutMessage.DistributeContent distributeContent = JSONUtil.toBean(str, OutMessage.DistributeContent.class);
                        ExternalMessage.DistributeContent content = new ExternalMessage.DistributeContent();
                        content.setDispatchId(distributeContent.getDispatchId());
                        content.setAgentId(distributeContent.getTargetAgentId());
                        content.setContent(List.of(Dict.create().set("type", "text").set("message", distributeContent.getCmd())));

                    } else if (StrUtil.equals(outMessage.getRole(), "reflection") && StrUtil.equals(outMessage.getType(), "reflect")) {
                        //反思
                        message.setRole("reflection");
                        message.setTo("agent");
                        message.setType("reflection");

                        String str = JSONUtil.toJsonStr(outMessage.getContent());
                        OutMessage.ReflectContent reflectContent = JSONUtil.toBean(str, OutMessage.ReflectContent.class);
                        ExternalMessage.ReflectContent content = new ExternalMessage.ReflectContent();
                        boolean fail = reflectContent.getOutput().stream().anyMatch(r -> r.getScore() <= 7);
                        content.setIsPass(!fail);
                        content.setAgentId(outMessage.getAgentId());
                        Matcher matcher = Pattern.compile("\"rawInput\":\"(.*?)\",\"rawOutput\":\"(.*?)\"").matcher(reflectContent.getInput());
                        if (matcher.find()) {
                            ExternalMessage.MessageScore messageScore = new ExternalMessage.MessageScore();
                            messageScore.setContent(List.of(Dict.create().set("type", "text").set("message", matcher.group(1))));
                            messageScore.setMessageType("text");
                            messageScore.setMessage(matcher.group(2));
                            messageScore.setReflectScoreList(reflectContent.getOutput());

                            content.setMessageScore(messageScore);
                        }
                        message.setContent(content);
                    } else if (StrUtil.equals(outMessage.getRole(), "assistant") && StrUtil.equals(outMessage.getType(), "think")) {
                        //大模型思考
                        message.setRole("assistant");
                        message.setTo("agent");
                        message.setType("reasoningContent");
                        message.setContent(outMessage.getContent());
                    }

                    if (StrUtil.isEmpty(message.getRole())) {
                        //部分消息不用返回
                        continue;
                    }

                    if (outMessage.getTokenUsage() != null) {
                        ExternalMessage.Usage usage = BeanUtil.copyProperties(outMessage.getTokenUsage(), ExternalMessage.Usage.class);
                        ExternalMessage.Completions completions = new ExternalMessage.Completions();
                        completions.setUsage(usage);
                        completions.setId(outMessage.getId());
                        completions.setModel(agentChatMessage.getModel());
                        message.setCompletions(completions);
                    }
                    if (isSubAgent(agentId, outMessage.getAgentId())) {
                        message.setRole("subagent");
                    }
                    resList.add(message);
                }
            });
        }
        return resList;
    }

    private boolean isSubAgent(String mainAgentId, String id) {
        return !StrUtil.equals(mainAgentId, id);
    }

    /**
     * 清空调试记录
     */
    public void clearChatData(String agentId, Integer debugFlag) {
        String userId = LoginContext.currentUserId();
        baseMapper.remove(new LambdaUpdateChainWrapper<>(baseMapper, AgentChatMessage.class)
                .eq(AgentChatMessage::getUserId, userId)
                .eq(AgentChatMessage::getAgentId, agentId)
                .eq(AgentChatMessage::getDebugFlag, debugFlag), AgentChatMessage.class);
    }
}