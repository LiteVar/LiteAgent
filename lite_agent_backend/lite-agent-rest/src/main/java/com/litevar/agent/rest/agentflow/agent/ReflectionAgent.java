package com.litevar.agent.rest.agentflow.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ReflectMessageInfo;
import com.litevar.agent.base.response.ReflectResult;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.agentflow.AgentSessionManager;
import com.litevar.agent.rest.agentflow.ExecutionStopManager;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 反思agent
 *
 * @author uncle
 * @since 2026/3/26 18:07
 */
@Slf4j
@Component
public class ReflectionAgent {
    @Resource
    private ExecutionStopManager stopManager;
    @Resource
    private AgentSessionManager manager;
    @Lazy
    @Resource
    private Orchestrator orchestrator;


    public String reflect(AgentContext context, String inputMsg, CompletionResponse llmOutput) {
        if (stopManager.isStopRequested(context.getRequestId())) {
            return null;
        }
        List<String> reflectionAgentIds = context.getRuntimeInfo().getReflectAgentIds();
        if (ObjectUtil.isEmpty(reflectionAgentIds)) {
            return null;
        }
        String outputMsg = llmOutput.getChoices().get(0).getMessage().getContent();
        String reflectionMsg = String.format("\"rawInput\":\"%s\",\"rawOutput\":\"%s\"", inputMsg, outputMsg);

        ReflectMessageInfo reflectionInfo = (ReflectMessageInfo) RedisUtil.getValue(String.format(CacheKey.REFLECT_INFO, context.getTaskId()));
        if (reflectionInfo == null) {
            reflectionInfo = new ReflectMessageInfo();
        }
        reflectionInfo.setReflectCount(reflectionInfo.getReflectCount() + 1);

        List<ReflectResult> resultList = new ArrayList<>();
        for (String agentId : reflectionAgentIds) {
            if (stopManager.shouldStop(context.getRequestId())) {
                return null;
            }
            String taskId = IdUtil.getSnowflakeNextIdStr();
            AgentExecutionSpec runtimeInfo = manager.getAgentRuntimeInfo(context.getSessionId(), agentId);
            orchestrator.agentSwitchMessage(context, taskId, agentId, runtimeInfo.getAgentName());

            CompletionResponse response = orchestrator.newTaskChat(context, agentId, taskId, List.of(UserMessage.of(reflectionMsg)));
            ReflectResult reflectResult;
            if (response != null) {
                reflectResult = JSONUtil.toBean(response.getChoices().get(0).getMessage().getContent(), ReflectResult.class);
            } else {
                reflectResult = new ReflectResult();
                reflectResult.setScore(-0.5d);
                reflectResult.setInformation("the reflect agent is not available");
            }
            resultList.add(reflectResult);
        }
        orchestrator.reflectMessage(context, inputMsg, outputMsg, resultList);

        if (resultList.stream().anyMatch(i -> i.getScore() <= 7)) {
            ReflectResult maxScore = resultList.stream().max(Comparator.comparing(ReflectResult::getScore)).get();
            if (maxScore.getScore() > reflectionInfo.getScore()) {
                reflectionInfo.setScore(maxScore.getScore());
                reflectionInfo.setOutput(outputMsg);
            }

            if (reflectionInfo.getReflectCount() == 10) {
                llmOutput.getChoices().get(0).getMessage().setContent(reflectionInfo.getOutput());
                log.info("[sessionId={},taskId={}]反思次数达到上限,取最高分数的记录作为输出", context.getSessionId(), context.getTaskId());
                return null;
            }
            log.info("[sessionId={},taskId={}]第{}次反思不通过,继续反思", context.getSessionId(), context.getTaskId(), reflectionInfo.getReflectCount());
            RedisUtil.setValue(String.format(CacheKey.REFLECT_INFO, context.getTaskId()), reflectionInfo, 10, TimeUnit.MINUTES);
            String reflectionResultMsg = resultList.stream().filter(i -> i.getScore() >= 0)
                    .map(ReflectResult::getInformation).collect(Collectors.joining("\n"));
            if (StrUtil.isEmpty(reflectionResultMsg)) {
                return null;
            }
            return reflectionResultMsg;
        }
        return null;
    }
}
