package com.litevar.agent.rest.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentPlanningDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ExecuteMode;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.agent.AgentApiKeyService;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.local.LocalFunctionService;
import com.litevar.agent.core.module.local.LocalModelService;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.openai.ObjectMapperSingleton;
import com.litevar.agent.openai.completion.ChatContext;
import com.litevar.agent.openai.completion.ChatModelRequest;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.JsonSchemaResponseFormat;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.DeveloperMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.openai.tool.*;
import com.litevar.agent.rest.openai.agent.AgentManager;
import com.litevar.agent.rest.openai.agent.AgentMsgType;
import com.litevar.agent.rest.openai.agent.MultiAgent;
import com.litevar.agent.rest.openai.message.AgentSwitchMessage;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2025/3/11 14:10
 */
@Slf4j
@Component
public class AgentUtil {
    private static JsonSchemaResponseFormat reflectResponseFormat;

    @Autowired
    private AgentService agentService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ToolService toolService;
    @Autowired
    private ToolFunctionService toolFunctionService;
    @Autowired
    private LocalModelService localModelService;
    @Autowired
    private LocalFunctionService localFunctionService;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private AgentApiKeyService agentApiKeyService;
    @Autowired
    private ChatContext chatContext;

    private static final Pattern jsonPattern = Pattern.compile("```json\\n([\\s\\S]*?)\\n```");

    /**
     * 获取反思的response-format
     */
    public static JsonSchemaResponseFormat getReflectResponseFormat() {
        if (reflectResponseFormat == null) {
            Map<String, JsonSchemaElement> properties = new HashMap<>();
            JsonStringSchema information = new JsonStringSchema();
            information.setDescription("校验结论");
            properties.put("information", information);

            JsonIntegerSchema score = new JsonIntegerSchema();
            score.setDescription("表示判断结果的得分，范围为0到10;");
            properties.put("score", score);

            JsonObjectSchema obj = new JsonObjectSchema();
            obj.setProperties(properties);
            obj.setRequired(List.of("information", "score"));

            JsonSchemaResponseFormat.JsonSchema jsonSchema = new JsonSchemaResponseFormat.JsonSchema();
            jsonSchema.setSchema(obj);
            jsonSchema.setName("response");

            reflectResponseFormat = new JsonSchemaResponseFormat();
            reflectResponseFormat.setJsonSchema(jsonSchema);
        }
        return reflectResponseFormat;
    }

    public MultiAgent buildAgent(Agent agent, List<String> datasetIds, String sessionId) {
        if (StrUtil.isEmpty(agent.getLlmModelId())) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }
        boolean localFlag = (agent instanceof LocalAgent);

        //model
        LlmModel model = localFlag ? localModelService.getById(agent.getLlmModelId())
                : modelService.findById(agent.getLlmModelId());
        if (!localFlag) {
            checkChatData(agent, model);
        }
        log.info("[sessionId={},agentId={},agentName={},initSession model] {},{}", sessionId, agent.getId(), agent.getName(), model.getName(), model.getBaseUrl());

        String contextId = IdUtil.getSnowflakeNextIdStr();
        log.info("[sessionId={},agentId={},agentName={},initSession contextId] {}", sessionId, agent.getId(), agent.getName(), contextId);

        //tool
        List<ToolSpecification> toolSpecificationList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(datasetIds)) {
            toolSpecificationList.add(FunctionUtil.knowledgeToTool());
        }
        if (Boolean.TRUE.equals(agent.getAutoAgentFlag())) {
            //auto agent
            toolSpecificationList.add(FunctionUtil.autoAgentToTool());
            long count = modelService.count(modelService.lambdaQuery()
                    .eq(LlmModel::getWorkspaceId, agent.getWorkspaceId())
                    .eq(LlmModel::getType, "LLM")
                    .eq(LlmModel::getAutoAgent, true));
            if (count == 0) {
                throw new ServiceException(ServiceExceptionEnum.AUTO_AGENT_WITHOUT_MODEL_TO_USE);
            }
        }

        if (ObjectUtil.isNotEmpty(agent.getFunctionList())) {
            List<String> functionIds = agent.getFunctionList().stream().map(Agent.AgentFunction::getFunctionId).toList();
            List<ToolFunction> functionList = localFlag ? localFunctionService.getByIds(functionIds).stream().map(i -> (ToolFunction) i).toList()
                    : toolFunctionService.getByIds(functionIds);
            toolSpecificationList.addAll(FunctionUtil.buildTool(functionList));
        }

        ChatModelRequest request = new ChatModelRequest();
        request.setBaseUrl(model.getBaseUrl());
        request.setApiKey(model.getApiKey());
        request.setModel(model.getName());
        request.setTemperature(agent.getTemperature());
        request.setTopP(agent.getTopP());
        request.setMaxCompletionTokens(agent.getMaxTokens());
        request.setContextId(contextId);

        MultiAgent agentInstance = new MultiAgent(agent.getId(), sessionId, request);
        List<String> sequence = ObjectUtil.isNotEmpty(agent.getSequence()) ? agent.getSequence() : Collections.emptyList();
        agentInstance.setSequence(sequence);
        agentInstance.setDatasetIds(datasetIds);
        agentInstance.setAgentName(agent.getName());
        agentInstance.setAgentType(agent.getType());
        agentInstance.setExecuteMode(agent.getMode());

        //反思agent没有子agent,可以直接跳过
        if (ObjectUtil.notEqual(agent.getType(), AgentType.REFLECTION.getType())
                && ObjectUtil.isNotEmpty(agent.getSubAgentIds())) {
            List<Agent> subAgentList = agentService.list(agentService.lambdaQuery()
                    .in(Agent::getId, agent.getSubAgentIds())
                    .eq(Agent::getStatus, 1));
            boolean flag = subAgentList.stream().filter(i -> ObjectUtil.equal(i.getType(), AgentType.REFLECTION.getType())).count() > 5;
            if (flag) {
                throw new ServiceException(ServiceExceptionEnum.REFLECT_AGENT_OVERSIZE);
            }
            subAgentList.forEach(a -> {
                List<String> subDsIds = agentDatasetRelaService.listDatasets(a.getId()).parallelStream().map(Dataset::getId).toList();
                MultiAgent subAgentInstance = buildAgent(a, subDsIds, sessionId);
                if (ObjectUtil.equal(subAgentInstance.getAgentType(), AgentType.REFLECTION.getType())) {
                    //子agent为反思agent
                    subAgentInstance.getRequest().setResponseFormat(AgentUtil.getReflectResponseFormat());
                    agentInstance.getReflectAgentMap().put(subAgentInstance.getAgentId(), subAgentInstance);

                } else if (ObjectUtil.equal(agentInstance.getAgentType(), AgentType.DISTRIBUTE.getType())) {
                    agentInstance.getDistributeAgentMap().put(subAgentInstance.getAgentId(), subAgentInstance);

                } else if (ObjectUtil.equal(agentInstance.getAgentType(), AgentType.GENERAL.getType())) {
                    agentInstance.getGeneralAgentMap().put(subAgentInstance.getAgentId(), subAgentInstance);
                }
            });

            if (!agentInstance.getDistributeAgentMap().isEmpty()) {
                Collection<MultiAgent> values = agentInstance.getDistributeAgentMap().values();
                toolSpecificationList.add(FunctionUtil.agentToTool(values));
            }
        }

        if (!toolSpecificationList.isEmpty()) {
            //兼容deekseek,tool为空时传null
            request.setTools(toolSpecificationList);
            log.info("[sessionId={},agentId={},agentName={},initSession function] {}", sessionId, agent.getId(), agent.getName(), JSONUtil.toJsonStr(toolSpecificationList));
        }

        cacheData(contextId, agent);

        return agentInstance;
    }

    public void cacheData(String contextId, Agent agent) {
        if (StrUtil.isNotBlank(agent.getPrompt())) {
            chatContext.add(contextId, List.of(DeveloperMessage.of(agent.getPrompt())));
        }
        if (ObjectUtil.isNotEmpty(agent.getFunctionList())) {
            Map<String, Object> functionModeMap = agent.getFunctionList().stream()
                    .collect(Collectors.toMap(Agent.AgentFunction::getFunctionId, Agent.AgentFunction::getMode));
            RedisUtil.setHashValue(String.format(CacheKey.FUNCTION_EXECUTE_MODE, contextId), functionModeMap, 1, TimeUnit.HOURS);
        }
    }

    private void checkChatData(Agent agent, LlmModel model) {
        //todo 检查agent链是否成环

        if (model == null) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }

        if (ObjectUtil.isNotEmpty(agent.getMaxTokens())) {
            Integer modelMaxTokens = ObjectUtil.isNotEmpty(model.getMaxTokens()) ? model.getMaxTokens() : 4096;
            if (agent.getMaxTokens() > modelMaxTokens) {
                throw new ServiceException(ServiceExceptionEnum.MAX_TOKEN_LARGER);
            }
        }
    }

    public String getAgentIdFromToken(String token) {
        //token不是以Bearer开头，则响应回格式不正确
        if (!token.startsWith(CommonConstant.JWT_TOKEN_PREFIX)) {
            throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
        }
        try {
            String apiKey = token.substring(CommonConstant.JWT_TOKEN_PREFIX.length() + 1);
            return agentApiKeyService.agentIdFromApiKey(apiKey);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
        }
    }

    public List<AgentPlanningDTO> planning(MultiAgent currentAgent, String taskId, boolean stream, String taskContent) {
        log.info("开始规划agent,sessionId={},任务:{}", currentAgent.getSessionId(), taskContent);
        //planning
        ChatModelRequest request = BeanUtil.copyProperties(currentAgent.getRequest(), ChatModelRequest.class, "tools");
        AgentPlanningDTO dto = new AgentPlanningDTO();
        dto.setName("planningAgent");
        AgentPlanningDTO.PlanDescription description = new AgentPlanningDTO.PlanDescription();
        description.setDuty("# 1.角色:任务拆解专家\n # 2.职责:根据用户指令拆解成1到多个子任务");

        String planningPrompt = String.format(ResourceUtil.readUtf8Str("classpath:prompt/planning.txt"),
                listCandidateModels(currentAgent.getAgentId()),
                listCandidateTools(currentAgent.getAgentId()));

        description.setConstraint(planningPrompt);
        dto.setDescription(description);

        MultiAgent planAgent = autoCreateAgent("tmp-" + IdUtil.getSnowflakeNextIdStr(), currentAgent.getSessionId(), request, dto);

        AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG,
                new AgentSwitchMessage(planAgent.getSessionId(), taskId, planAgent.getAgentId(), planAgent.getAgentName()));

        List<Message> submitMsg = List.of(UserMessage.of(taskContent));
        CompletionResponse planResult = AgentManager.chat(planAgent, taskId, submitMsg, stream).get(planAgent.getAgentId());
        if (planResult != null) {
            return resolvePlanning(planResult.getChoices().get(0).getMessage().getContent());
        }
        return null;
    }

    public List<MultiAgent> createAgent(List<AgentPlanningDTO> taskList, MultiAgent currentAgent) {
        List<MultiAgent> executeAgentList = new ArrayList<>();
        taskList.forEach(task -> {
            String id = "tmp-" + IdUtil.getSnowflakeNextIdStr();
            ChatModelRequest r = new ChatModelRequest();
            MultiAgent subAgent = autoCreateAgent(id, currentAgent.getSessionId(), r, task);
            executeAgentList.add(subAgent);
        });
        return executeAgentList;
    }

    public String executeAgent(List<MultiAgent> executeAgentList, String taskId, boolean stream) {
        //execute agent
        //<agentId,result>
        Map<String, CompletionResponse> result = new HashMap<>();
        executeAgentList.forEach(agent -> {
            //切换到子agent
            AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG,
                    new AgentSwitchMessage(agent.getSessionId(), taskId, agent.getAgentId(), agent.getAgentName()));

            List<Message> msg = List.of(UserMessage.of(agent.getAgentName()));

            Map<String, CompletionResponse> map = AgentManager.chat(agent, taskId, msg, stream);
            result.putAll(map);
        });
        List<Dict> arr = new ArrayList<>();
        collect(executeAgentList, result, arr);
        return JSONUtil.toJsonStr(arr);
    }

    public void summary(String taskAndResult, MultiAgent currentAgent, String taskId, boolean stream) {
        AgentPlanningDTO.PlanDescription description = new AgentPlanningDTO.PlanDescription();
        description.setDuty("#1.角色:任务总结专家\n #2.职责:对任务进行总结");
        description.setConstraint(ResourceUtil.readUtf8Str("classpath:prompt/summary.txt"));

        AgentPlanningDTO dto = new AgentPlanningDTO();
        dto.setName("summaryAgent");
        dto.setDescription(description);
        ChatModelRequest request = BeanUtil.copyProperties(currentAgent.getRequest(), ChatModelRequest.class, "tools");
        //pAgentId设为0,执行完可以断开sse连接
        MultiAgent summaryAgent = autoCreateAgent("tmp-" + IdUtil.getSnowflakeNextIdStr(), currentAgent.getSessionId(), request, dto);

        AgentManager.handleMsg(AgentMsgType.SWITCH_AGENT_MSG,
                new AgentSwitchMessage(summaryAgent.getSessionId(), taskId, summaryAgent.getAgentId(), summaryAgent.getAgentName()));

        Map<String, CompletionResponse> map = AgentManager.chat(summaryAgent, taskId, List.of(UserMessage.of(taskAndResult)), stream);
        if (ObjectUtil.isNotEmpty(map)) {
            CompletionResponse res = map.get(summaryAgent.getAgentId());
            //把总结添加到主agent的上下文中
            AssistantMessage assistantMessage = res.getChoices().get(0).getMessage();
            chatContext.add(currentAgent.getContextId(), List.of(assistantMessage));
        }
    }

    private void collect(Collection<MultiAgent> agentList, Map<String, CompletionResponse> result, List<Dict> arr) {
        agentList.forEach(agent -> {
            CompletionResponse r = result.get(agent.getAgentId());
            String task = agent.getAgentName();
            String resContent = "";
            if (r != null) {
                resContent = r.getChoices().get(0).getMessage().getContent();
            }
            arr.add(Dict.of("task", task, "result", resContent));
            if (ObjectUtil.isNotEmpty(agent.getGeneralAgentMap())) {
                collect(agent.getGeneralAgentMap().values(), result, arr);
            }
        });
    }

    /**
     * 解析出markdown中的json
     * 输出规划信息
     */
    private List<AgentPlanningDTO> resolvePlanning(String content) {
        Matcher matcher = jsonPattern.matcher(content);
        if (matcher.find()) {
            String json = matcher.group(1);
            if (StrUtil.startWith(json, "{")) {
                json = "[" + json + "]";
            }
            try {
                AgentPlanningDTO[] dto = ObjectMapperSingleton.getObjectMapper().readValue(json, AgentPlanningDTO[].class);
                return Arrays.asList(dto);

            } catch (Exception e) {
                log.error("json解析异常", e);
            }
        } else {
            log.error("json匹配失败:{}", content);
        }
        return Collections.emptyList();
    }

    private MultiAgent autoCreateAgent(String agentId, String sessionId, ChatModelRequest request, AgentPlanningDTO dto) {
        //model
        if (ObjectUtil.isNotEmpty(dto.getModel())) {
            LlmModel model = modelService.getById(dto.getModel().getId());
            request.setBaseUrl(model.getBaseUrl());
            request.setApiKey(model.getApiKey());
            request.setModel(model.getName());
        }
        log.info("[自动创建agent agentName={} model] {},{}", dto.getName(), request.getModel(), request.getBaseUrl());
        request.setContextId(IdUtil.getSnowflakeNextIdStr());
        log.info("[自动创建agent agentName={} contextId] {}", dto.getName(), request.getContextId());

        //tool
        if (ObjectUtil.isNotEmpty(dto.getTools())) {
            List<String> toolIds = dto.getTools().stream().map(AgentPlanningDTO.PlanTool::getId).toList();
            List<ToolFunction> functionList = toolFunctionService.list(toolFunctionService.lambdaQuery().in(ToolFunction::getToolId, toolIds));
            List<ToolSpecification> toolSpecificationList = FunctionUtil.buildTool(functionList);
            if (!toolSpecificationList.isEmpty()) {
                request.setTools(toolSpecificationList);
                log.info("[自动创建agent agentName={} function] {}", dto.getName(), JSONUtil.toJsonStr(toolSpecificationList));
                cacheAgentData(request.getContextId(), functionList);
            }
        }

        MultiAgent agent = new MultiAgent(agentId, sessionId, request);
        agent.setSequence(Collections.emptyList());
        agent.setAgentName(dto.getName());
        agent.setAgentType(AgentType.DISTRIBUTE.getType());
        agent.setExecuteMode(ExecuteMode.PARALLEL.getMode());

        String prompt = "duty:" + dto.getDescription().getDuty() + "\n constraint:" + dto.getDescription().getConstraint();
        log.info("[自动创建agent agentName={} prompt] {}", dto.getName(), prompt);
        chatContext.add(request.getContextId(), List.of(DeveloperMessage.of(prompt)));

        if (ObjectUtil.isNotEmpty(dto.getChildren())) {
            Map<String, MultiAgent> subAgentMap = new HashMap<>();
            dto.getChildren().forEach(child -> {
                String id = "tmp-" + IdUtil.getSnowflakeNextId();
                ChatModelRequest r = new ChatModelRequest();
                MultiAgent subAgent = autoCreateAgent(id, sessionId, r, child);
                subAgentMap.put(id, subAgent);
            });
            agent.setGeneralAgentMap(subAgentMap);
        }
        return agent;
    }

    private void cacheAgentData(String contextId, List<ToolFunction> functionList) {
        Map<String, Object> functionModeMap = functionList.stream()
                .collect(Collectors.toMap(ToolFunction::getId, i -> ExecuteMode.PARALLEL.getMode()));
        RedisUtil.setHashValue(String.format(CacheKey.FUNCTION_EXECUTE_MODE, contextId), functionModeMap, 1, TimeUnit.HOURS);
    }

    /**
     * 查询候选模型列表
     */
    private String listCandidateModels(String agentId) {
        Agent agent = agentService.findById(agentId);
        //获取当前工作空间的所有模型,并去重(url,key,name相同视为一个),组装成:[{id:123,name:qwen3-32b}]
        List<Dict> list = modelService.list(modelService.lambdaQuery()
                        .eq(LlmModel::getAutoAgent, true)
                        .eq(LlmModel::getType, "LLM")
                        .eq(LlmModel::getWorkspaceId, agent.getWorkspaceId()))
                .stream()
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

    /**
     * 获取候选工具集列表
     */
    private String listCandidateTools(String agentId) {
        Agent agent = agentService.findById(agentId);
        //获取当前工作空间的所有工具集,组装成:[{id:123,name:xxx,desc:xxx}]
        List<Dict> list = toolService.list(toolService.lambdaQuery()
                        .projectDisplay(ToolProvider::getId, ToolProvider::getName, ToolProvider::getDescription)
                        .eq(ToolProvider::getAutoAgent, true)
                        .eq(ToolProvider::getWorkspaceId, agent.getWorkspaceId()))
                .stream().map(i -> Dict.create().set("id", i.getId()).set("name", i.getName()).set("desc", i.getDescription())).toList();
        return JSONUtil.toJsonStr(list);
    }

}
