package com.litevar.agent.rest.agentflow.agent;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.openai.ObjectMapperSingleton;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.agentflow.AgentSessionManager;
import com.litevar.agent.rest.agentflow.auto.execution.PlanExecutionBootstrap;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * agent规划
 *
 * @author uncle
 * @since 2026/3/16 16:54
 */
@Slf4j
@Component
public class PlanningAgent {
    private static final long PLANNING_AGENT_LOCK_TTL_SECONDS = 10;
    private static final long PLANNING_AGENT_LOCK_WAIT_MILLIS = 100;
    private static final int PLANNING_AGENT_LOCK_MAX_RETRY = 100;
    private static final String PROMPT = """
            # 1.角色：你是一个专职的PlanningAgent
            你的职责是理解用户目标,为后续执行阶段生成精简、清晰、可执行的DAG任务计划。
            你只负责规划,不负责直接执行任务。
            
            # 2.执行环境认知（必须理解）
            - 每个任务节点后续都会交给一个专门的执行Agent处理
            - 单个执行Agent可以在一个节点内使用多个工具,因此不要按“一个工具一个节点”进行机械拆分
            - 你的目标是生成更少但功能完整的节点,让每个节点都具有明确业务目标和独立价值
            - 当一个节点不需要工具时,tools数组必须为空,禁止为了显得完整而绑定无关工具
            - 后续执行阶段可能根据已有结果提前结束,因此不要规划低价值、弱收益、仅做表面润色的节点
            
            # 3.核心分解原则（严格遵循）
            ## 3.1 最小依赖原则
            - 只在存在真实逻辑依赖时才建立dependencies关系,避免不必要的串行约束
            - 每个任务的dependencies数组只包含直接前置依赖,不包含间接依赖
            
            ## 3.2 关键路径优化原则
            - 通过DAG思维最小化关键路径长度
            - 避免为关键路径任务添加不必要的dependencies依赖
            - 能并行就并行,不要把本可独立执行的节点串起来
            
            ## 3.3 功能完整性优先原则
            - 基于业务目标而不是工具数量进行分解
            - 每个节点都应回答“这个节点完成后,对整体任务推进了什么”
            - 如果两个步骤天然属于同一业务动作且适合一起完成,应合并成一个节点
            
            ## 3.4 节点价值原则
            - 只保留完成用户目标所必需或明显高价值的节点
            - 不要为了看起来完整而加入可有可无的补充节点
            - 节点数量应尽量少,但不能牺牲任务正确性
            
            # 4.规划目标
            根据用户指令生成一个DAG任务计划。
            输出中的每个对象代表一个待执行节点,不是父子树中的子agent。
            必须基于上述原则进行科学分解,按以下JSON数组格式返回一个markdown json代码块：
            [{"id":"1","name":"{name}-agent","description":{"duty":"duty","constraint":"constraint"},"tools":[{"id":"0","name":"tool name"},{"id":"1","name":"tool name"}],"model":{"id":"0","name":"model name"},"dependencies":[]},{"id":"2","name":"{name}-agent","description":{"duty":"duty","constraint":"constraint"},"tools":[{"id":"2","name":"tool name"}],"model":{"id":"0","name":"model name"},"dependencies":[{"id":"1","name":"{name}-agent"}]}]
            
            # 5.详细要求
            ## 5.1 节点字段要求
            - id必须在本次规划结果内唯一,且dependencies中引用的id必须真实存在
            - name应简洁明确,能够体现节点职责
            - 每个节点都必须可独立执行,不要生成语义空泛的占位节点
            
            ## 5.2 描述(description)：精确描述任务职责和约束
            - duty字段要求：不得少于10个字,必须包含用户指令中的关键参数和具体目标,写清楚“做什么、怎么做、产出什么”
            - constraint字段要求：明确执行约束、质量标准、成功条件和边界限制
            
            ## 5.3 智能模型选择(model)：根据任务复杂度选择最优模型
            availableModels:%s
            - 选择原则：复杂分析任务选择高性能模型,简单执行任务选择轻量级模型
            - 当一个节点需要调用工具时,必须选择支持tool call的模型
            - 如果某个模型标记为不支持tool call,则该模型对应的节点tools必须为空数组
            - 不允许出现“模型不支持tool call,但节点仍分配了工具”的情况
            - 确保所有model的id和name都与availableModels中的值完全一致
            
            ## 5.4 智能工具分配(tools)
            availableTools:%s
            - 只给真正需要工具的节点分配工具
            - 工具分配应服务于节点职责,禁止分配语义无关的工具
            - 如果一个节点不需要工具,tools必须为空数组
            - 确保所有tools的id和name都与availableTools中的值完全一致
            
            ## 5.5 DAG质量自检（必须执行）
            分解完成后，请对结果进行以下检查：
            - 循环依赖检查：验证DAG结构无环,确保不存在A→B→A
            - 拓扑有效性：验证所有dependencies引用的任务ID都存在且有效
            - 冗余检查：验证不存在职责高度重复的节点
            - 工具合理性检查：验证tools分配与节点职责一致
            - 模型-工具兼容性检查：凡是tools非空的节点,其model必须支持tool call; 凡是model不支持tool call的节点,tools必须为空数组
            - 收敛检查：验证整个计划能够收敛到用户目标,而不是无关扩展
            
            **严格要求**：
            - 使用用户指令相同的语言进行规划
            - 输出必须是一个```json代码块,代码块内只包含紧凑JSON数组,不要输出任何额外解释
            - 不要把计划组织成树结构,不要输出children字段
            - 必须输出dependencies字段,无依赖时使用空数组
            - 确保DAG结构无环,验证不存在循环依赖
            """;
    public static final String planningAgentId = "PlanningAgent";
    private static final Pattern JSON_PATTERN = Pattern.compile("```json\\n([\\s\\S]*?)\\n```");

    @Resource
    private AgentService agentService;
    @Resource
    private ModelService modelService;
    @Resource
    private ToolService toolService;
    @Resource
    private Orchestrator orchestrator;
    @Resource
    private AgentSessionManager manager;
    @Resource
    private PlanExecutionBootstrap planExecutionBootstrap;

    public String planning(AgentContext context, String callId, String taskContent) {
        List<AgentPlanningDTO> planningResult = null;
        try {
            planningResult = planningInternal(context, taskContent);
        } catch (ServiceException ex) {
            log.error("[planning error] sessionId={},requestId={},taskId={},planning result:{}",
                    context.getSessionId(), context.getRequestId(), context.getTaskId(), ex.getMessage());
            //再试一次
            try {
                planningResult = planningInternal(context, ex.getMessage());
            } catch (ServiceException ex2) {
                log.error("[planning error] sessionId={},requestId={},taskId={},planning result:{},试了两次都规划失败,退出规划",
                        context.getSessionId(), context.getRequestId(), context.getTaskId(), ex.getMessage());
            }
        }
        if (planningResult == null) {
            return "planning failure";
        }

        String planId = callId;
        planExecutionBootstrap.savePlan(planId, taskContent, planningResult);
        orchestrator.planMessage(context, planId, planningResult);
        return null;
    }

    private List<AgentPlanningDTO> planningInternal(AgentContext context, String taskContent) {
        log.info("planning agent,sessionId={},requestId={},taskId={},task:{}",
                context.getSessionId(), context.getRequestId(), context.getTaskId(), taskContent);

        AgentExecutionSpec planningAgent = getPlanningAgent(context);
        String taskId = IdUtil.getSnowflakeNextIdStr();
        orchestrator.agentSwitchMessage(context, taskId, planningAgent.getAgentId(), planningAgent.getAgentName());

        CompletionResponse planResult = orchestrator.newTaskChat(context, planningAgent.getAgentId(),
                taskId, List.of(UserMessage.of(taskContent)));
        List<AgentPlanningDTO> taskList = resolvePlanning(planResult);
        if (taskList.isEmpty()) {
            throw new ServiceException("Planning json parsing failed, please output json in the correct format and the correct data.");
        }
        String tip = checkCircle(taskList);
        if (StrUtil.isNotBlank(tip)) {
            throw new ServiceException(tip);
        }

        return taskList;
    }

    private AgentExecutionSpec getPlanningAgent(AgentContext context) {
        String sessionId = context.getSessionId();
        AgentExecutionSpec planningAgent = manager.getAgentRuntimeInfo(sessionId, planningAgentId);
        if (planningAgent != null) {
            return planningAgent;
        }
        String lockKey = context.getSessionId();
        if (Boolean.TRUE.equals(RedisUtil.setNx(lockKey, "1", PLANNING_AGENT_LOCK_TTL_SECONDS, TimeUnit.SECONDS))) {
            try {
                planningAgent = manager.getAgentRuntimeInfo(sessionId, planningAgentId);
                if (planningAgent != null) {
                    return planningAgent;
                }

                Agent autoAgent = agentService.getById(context.getAgentId());
                Agent agent = new Agent();
                agent.setId(planningAgentId);
                agent.setName("PlanningAgent");
                agent.setPrompt(getPlanPrompt(autoAgent.getWorkspaceId()));
                agent.setLlmModelId(context.getRuntimeInfo().getRequest().getLlmModelId());
                agent.setTemperature(context.getRuntimeInfo().getRequest().getTemperature());
                agent.setTopP(context.getRuntimeInfo().getRequest().getTopP());
                agent.setMaxTokens(context.getRuntimeInfo().getRequest().getMaxCompletionTokens());
                agent.setTurns(context.getRuntimeInfo().getRequest().getTurns());

                return manager.addTmpAgent(agent, sessionId);
            } finally {
                RedisUtil.delKey(lockKey);
            }
        }
        for (int i = 0; i < PLANNING_AGENT_LOCK_MAX_RETRY; i++) {
            planningAgent = manager.getAgentRuntimeInfo(sessionId, planningAgentId);
            if (planningAgent != null) {
                return planningAgent;
            }
            try {
                Thread.sleep(PLANNING_AGENT_LOCK_WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("PlanningAgent initialization interrupted");
            }
        }
        throw new ServiceException("PlanningAgent initialization timeout");
    }

    private String getPlanPrompt(String workspaceId) {
        return String.format(PROMPT, listCandidateModels(workspaceId), listCandidateTools(workspaceId));
    }

    private String listCandidateModels(String workspaceId) {
        //获取当前工作空间和系统的所有模型,并去重(url,key,name相同视为一个),组装成:[{id:123,name:qwen3-32b}]
        List<Dict> list = modelService.lambdaQuery()
                .eq(LlmModel::getAutoAgent, true)
                .eq(LlmModel::getType, "LLM")
                .in(LlmModel::getWorkspaceId, workspaceId, "0").list()
                .stream()
                .filter(i -> !i.getBaseUrl().contains("{{<ENDPOINT>}}"))
                .collect(Collectors.groupingBy(i -> i.getBaseUrl() + i.getApiKey() + i.getName()))
                .values().stream()
                .map(i -> {
                    LlmModel model = i.get(0);
                    Dict dict = Dict.create().set("id", model.getId()).set("name", model.getName());
                    String desc = "";
                    if (!model.getToolInvoke()) {
                        desc = "this model does not support tool calls";
                    }
                    dict.set("description", desc);
                    return dict;
                }).toList();
        return JSONUtil.toJsonStr(list);
    }

    private String listCandidateTools(String workspaceId) {
        //获取当前工作空间的所有工具集,组装成:[{id:123,name:xxx,desc:xxx}]
        List<Dict> list = toolService.lambdaQuery()
                .projectDisplay(ToolProvider::getId, ToolProvider::getName, ToolProvider::getDescription)
                .eq(ToolProvider::getAutoAgent, true)
                .eq(ToolProvider::getWorkspaceId, workspaceId).list()
                .stream().map(i -> Dict.create().set("id", i.getId()).set("name", i.getName()).set("desc", i.getDescription()))
                .toList();
        return JSONUtil.toJsonStr(list);
    }

    private List<AgentPlanningDTO> resolvePlanning(CompletionResponse planResult) {
        if (planResult == null) {
            return Collections.emptyList();
        }
        String content = planResult.getChoices().get(0).getMessage().getContent();
        Matcher matcher = JSON_PATTERN.matcher(content);
        if (matcher.find()) {
            String jsonStr = matcher.group(1);
            if (JSONUtil.isTypeJSON(jsonStr)) {
                try {
                    AgentPlanningDTO[] dto = ObjectMapperSingleton.getObjectMapper().readValue(jsonStr, AgentPlanningDTO[].class);
                    return Arrays.asList(dto);
                } catch (JsonProcessingException e) {
                    log.error("json解析异常", e);
                }
            }
        } else {
            log.error("json匹配失败:{}", content);
        }
        return Collections.emptyList();
    }

    /**
     * 检查任务是否循环依赖
     */
    private String checkCircle(List<AgentPlanningDTO> taskList) {
        Map<String, AgentPlanningDTO> taskMap = taskList.stream()
                .collect(Collectors.toMap(AgentPlanningDTO::getId, i -> i));
        Deque<String> visited = new ArrayDeque<>();
        for (String taskId : taskMap.keySet()) {
            if (checkCircle(taskMap, taskId, visited)) {
                String chain = String.join(" -> ", visited);
                return "Circular dependency detected: " + chain;
            }
            visited.clear();
        }
        return null;
    }

    private boolean checkCircle(Map<String, AgentPlanningDTO> taskMap, String taskId, Deque<String> visited) {
        if (visited.contains(taskId)) {
            visited.addLast(taskId);
            //循环依赖
            return true;
        }
        visited.addLast(taskId);
        AgentPlanningDTO dto = taskMap.get(taskId);
        if (dto != null && ObjectUtil.isNotEmpty(dto.getDependencies())) {
            for (AgentPlanningDTO.Dependencies dependencyTask : dto.getDependencies()) {
                if (checkCircle(taskMap, dependencyTask.getId(), visited)) {
                    return true;
                }
                visited.pollLast();
            }
        }
        return false;
    }
}
