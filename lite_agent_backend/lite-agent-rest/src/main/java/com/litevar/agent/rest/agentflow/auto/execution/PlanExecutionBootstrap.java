package com.litevar.agent.rest.agentflow.auto.execution;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ExecuteMode;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.rest.agentflow.AgentSessionManager;
import com.litevar.agent.rest.agentflow.bean.PlanAgentState;
import com.litevar.agent.rest.agentflow.bean.PlanAgentStatus;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 计划结果的执行态初始化
 *
 * @author uncle
 * @since 2026/03/18 18:29
 */
@Component
public class PlanExecutionBootstrap {
    @Resource
    private ToolFunctionService toolFunctionService;
    @Resource
    private AgentSessionManager manager;

    /**
     * 读取规划结果
     *
     * @param planId 规划ID
     * @return 规划节点列表
     */
    @SuppressWarnings("unchecked")
    public PlanInfo loadPlan(String planId) {
        Object value = RedisUtil.getValue(String.format(CacheKey.SESSION_PLAN_INFO, planId));
        if (value == null) {
            return null;
        }
        return (PlanInfo) value;
    }

    public void savePlan(String planId, String originTask, List<AgentPlanningDTO> taskList) {
        PlanInfo planInfo = new PlanInfo(originTask, taskList);
        RedisUtil.setValue(String.format(CacheKey.SESSION_PLAN_INFO, planId), planInfo, 30, TimeUnit.MINUTES);
    }

    /**
     * 初始化计划执行态
     *
     * @param sessionId 会话ID
     * @param planId    规划ID
     * @return 执行态
     */
    public PlanExecutionState initState(String sessionId, String planId) {
        PlanExecutionState cacheState = getState(sessionId, planId);
        if (cacheState != null) {
            return cacheState;
        }
        PlanExecutionBootstrap.PlanInfo planInfo = loadPlan(planId);
        if (planInfo == null) {
            return null;
        }
        PlanExecutionState state = new PlanExecutionState();
        state.setPlanId(planId);

        List<PlanAgentState> agents = new ArrayList<>(planInfo.getTaskList().size());
        List<AgentPlanningDTO> taskList = planInfo.getTaskList();
        Map<String, String> idMapping = new HashMap<>(taskList.size());
        taskList.forEach(i -> idMapping.put(i.getId(), "tmp-" + IdUtil.getSnowflakeNextIdStr()));

        for (AgentPlanningDTO task : taskList) {
            PlanAgentState agentState = new PlanAgentState();
            String agentId = idMapping.get(task.getId());
            agentState.setAgentId(agentId);
            agentState.setAgentName(task.getName());
            if (task.getDescription() != null) {
                agentState.setDuty(task.getDescription().getDuty());
                agentState.setConstraint(task.getDescription().getConstraint());
            }
            if (ObjectUtil.isNotEmpty(task.getDependencies())) {
                agentState.setDependencyAgentIds(task.getDependencies().stream()
                        .map(AgentPlanningDTO.Dependencies::getId)
                        .map(idMapping::get)
                        .filter(ObjectUtil::isNotEmpty)
                        .toList());
                agentState.setStatus(PlanAgentStatus.PENDING);
            } else {
                agentState.setStatus(PlanAgentStatus.READY);
            }

            Agent agent = new Agent();
            agent.setId(agentId);
            agent.setName(agentState.getAgentName());
            if (task.getModel() != null) {
                agent.setLlmModelId(task.getModel().getId());
            }
            String duty = "";
            if (StrUtil.isNotBlank(agentState.getDuty())) {
                duty = "duty:\n" + agentState.getDuty();
            }
            String constraint = "";
            if (StrUtil.isNotBlank(agentState.getConstraint())) {
                constraint = "constraint:\n" + agentState.getConstraint();
            }
            agent.setPrompt(StrUtil.join("\n", duty, constraint));

            if (ObjectUtil.isNotEmpty(task.getTools())) {
                List<String> toolIds = task.getTools().stream().map(AgentPlanningDTO.PlanTool::getId).toList();
                List<String> functionIds = toolFunctionService.lambdaQuery()
                        .projectDisplay(ToolFunction::getId)
                        .in(ToolFunction::getToolId, toolIds).list()
                        .stream().map(ToolFunction::getId).toList();
                if (!functionIds.isEmpty()) {
                    List<Agent.AgentFunction> functionList = functionIds.stream().map(i -> {
                        Agent.AgentFunction function = new Agent.AgentFunction();
                        function.setFunctionId(i);
                        function.setMode(ExecuteMode.PARALLEL.getMode());
                        return function;
                    }).toList();
                    agent.setFunctionList(functionList);
                }
            }
            manager.addTmpAgent(agent, sessionId);
            agents.add(agentState);
        }
        state.setAgents(agents);
        saveState(sessionId, state);
        return state;
    }

    /**
     * 获取执行态
     *
     * @param sessionId 会话ID
     * @param planId    规划ID
     * @return 执行态
     */
    public PlanExecutionState getState(String sessionId, String planId) {
        return (PlanExecutionState) RedisUtil.getValue(String.format(CacheKey.PLAN_EXECUTION_STATE, sessionId, planId));
    }

    /**
     * 保存执行态
     *
     * @param sessionId 会话ID
     * @param state     执行态
     */
    public void saveState(String sessionId, PlanExecutionState state) {
        RedisUtil.setValue(String.format(CacheKey.PLAN_EXECUTION_STATE, sessionId, state.getPlanId()),
                state, AgentSessionManager.sessionExpireTime, TimeUnit.HOURS);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanInfo {
        private String originTask;
        private List<AgentPlanningDTO> taskList;
    }
}
