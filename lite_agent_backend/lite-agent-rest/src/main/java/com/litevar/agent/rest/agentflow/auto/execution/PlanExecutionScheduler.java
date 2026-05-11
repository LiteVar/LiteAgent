package com.litevar.agent.rest.agentflow.auto.execution;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.ExecutionStopManager;
import com.litevar.agent.rest.agentflow.agent.Orchestrator;
import com.litevar.agent.rest.agentflow.bean.PlanAgentState;
import com.litevar.agent.rest.agentflow.bean.PlanAgentStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划执行调度
 *
 * @author uncle
 * @since 2026/03/19 10:25
 */
@Component
public class PlanExecutionScheduler {
    @Resource
    private PlanExecutionBootstrap bootstrap;
    @Resource
    private Orchestrator orchestrator;
    @Resource
    private ExecutionStopManager stopManager;

    /**
     * 通过通用调度工具执行计划节点
     *
     * @param context 请求上下文
     * @param argObj  调度参数
     * @return 调度结果
     */
    public String dispatch(AgentContext context, JSONObject argObj) {
        String planId = argObj.getStr("planId");
        String action = argObj.getStr("action");
        if (StrUtil.isBlank(planId)) {
            return "planId is required";
        }
        if (!StrUtil.equalsAny(action, "EXECUTE_NODE", "EXECUTE_BATCH", "FINISH")) {
            return action + " is invalid action value";
        }

        PlanExecutionState state = bootstrap.getState(context.getSessionId(), planId);
        if (state == null) {
            return "planId state not found:" + planId;
        }

        JSONArray taskArray = argObj.getJSONArray("tasks");
        //要执行的节点<agentId,input>
        LinkedHashMap<String, String> taskInputMap = new LinkedHashMap<>();
        Map<String, PlanAgentState> allAgentMap = state.getAgents().stream()
                .collect(Collectors.toMap(PlanAgentState::getAgentId, i -> i));
        if (taskArray != null) {
            for (Object i : taskArray) {
                JSONObject task = JSONUtil.parseObj(i);
                String agentId = task.getStr("agentId");
                String input = task.getStr("input");
                if (StrUtil.isNotBlank(agentId) && StrUtil.isNotBlank(input)) {
                    PlanAgentState agentState = allAgentMap.get(agentId);
                    if (agentState == null) {
                        return "agentId does not exist in the current plan:" + agentId;
                    } else if (agentState.getStatus() != PlanAgentStatus.READY) {
                        return "agent is not ready: " + agentId;
                    }
                    taskInputMap.put(agentId, input);
                }
            }
        }
        if (!"FINISH".equals(action) && taskInputMap.isEmpty()) {
            return "there are no executable agents";
        }

        if ("FINISH".equals(action)) {
            //节点完成
            return result(state, "FINISH", Collections.emptyList(), null);
        }
        List<String> executedAgentIds = new ArrayList<>();
        List<Dict> executeResult = new ArrayList<>();
        for (Map.Entry<String, String> entry : taskInputMap.entrySet()) {
            if (stopManager.shouldStop(context.getRequestId())) {
                break;
            }
            PlanAgentState agentState = allAgentMap.get(entry.getKey());
            String output = executeNode(context, state, agentState, entry.getValue());
            executedAgentIds.add(agentState.getAgentId());
            if (StrUtil.isNotBlank(output)) {
                Dict dict = Dict.create().set("agentId", agentState.getAgentId()).set("agentName", agentState.getAgentName()).set("output", output);
                executeResult.add(dict);
            }
        }
        bootstrap.saveState(context.getSessionId(), state);
        return result(state, action, executedAgentIds, executeResult);
    }

    private String executeNode(AgentContext context, PlanExecutionState state, PlanAgentState agentState, String input) {
        agentState.setStatus(PlanAgentStatus.RUNNING);
        bootstrap.saveState(context.getSessionId(), state);

        String taskId = IdUtil.getSnowflakeNextIdStr();
        orchestrator.agentSwitchMessage(context, taskId, agentState.getAgentId(), agentState.getAgentName());
        CompletionResponse response = orchestrator.newTaskChat(context, agentState.getAgentId(), taskId,
                List.of(UserMessage.of(input)));
        agentState.setStatus(PlanAgentStatus.FINISHED);
        if (response != null && !response.getChoices().isEmpty() && response.getChoices().get(0).getMessage() != null) {
            agentState.setStatus(PlanAgentStatus.ACCEPTED);
            refreshReadyNodes(state);
            return response.getChoices().get(0).getMessage().getContent();
        }
        return null;
    }

    private void refreshReadyNodes(PlanExecutionState state) {
        Map<String, PlanAgentStatus> statusMap = state.getAgents().stream()
                .collect(Collectors.toMap(PlanAgentState::getAgentId, PlanAgentState::getStatus));
        for (PlanAgentState agentState : state.getAgents()) {
            if (agentState.getStatus() != PlanAgentStatus.PENDING) {
                continue;
            }
            boolean allAccepted = agentState.getDependencyAgentIds().stream()
                    .allMatch(i -> statusMap.get(i) == PlanAgentStatus.ACCEPTED);
            if (allAccepted) {
                //该节点的所有依赖节点都完成了,更新状态为ready
                agentState.setStatus(PlanAgentStatus.READY);
            }
        }
    }

    private String result(PlanExecutionState state, String action, List<String> executedAgentIds, List<Dict> output) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("planId", state.getPlanId());
        data.put("action", action);
        data.put("executedAgentIds", executedAgentIds);
        data.put("output", output);
        Map<String, List<Dict>> statusAgentMap = state.getAgents().stream().map(i -> Dict.create().set("agentId", i.getAgentId())
                .set("agentName", i.getAgentName()).set("dependencyAgentIds", i.getDependencyAgentIds())
                .set("status", i.getStatus().name())).collect(Collectors.groupingBy(i -> i.getStr("status")));

        data.put("readyAgents", statusAgentMap.remove(PlanAgentStatus.READY.name()));
        data.put("completedAgents", statusAgentMap.remove(PlanAgentStatus.ACCEPTED.name()));
        data.put("remainingAgents", statusAgentMap);
        return JSONUtil.toJsonStr(data);
    }
}
