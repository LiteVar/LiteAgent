package com.litevar.agent.rest.agentflow;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.litevar.agent.core.module.local.LocalFunctionService;
import com.litevar.agent.core.module.local.LocalModelService;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.openai.completion.ChatContext;
import com.litevar.agent.openai.completion.ChatModelRequest;
import com.litevar.agent.openai.completion.JsonSchemaResponseFormat;
import com.litevar.agent.openai.completion.message.DeveloperMessage;
import com.litevar.agent.openai.tool.*;
import com.litevar.agent.rest.agentflow.bean.AgentExecutionSpec;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AgentRuntimeFactory {
    @Resource
    private ModelService modelService;
    @Resource
    private LocalModelService localModelService;
    @Resource
    private ToolFunctionService toolFunctionService;
    @Resource
    private LocalFunctionService localFunctionService;
    @Resource
    private AgentService agentService;
    @Resource
    private LocalAgentService localAgentService;
    @Resource
    private AgentDatasetRelaService agentDatasetRelaService;
    @Resource
    private ChatContext chatContext;

    public static final String TOOL_KNOWLEDGE_BASE = "knowledgeBase";
    public static final String TOOL_AGENT_DISTRIBUTE = "agentDistribute";
    public static final String TOOL_AUTO_AGENT = "PlanningAgent";
    public static final String TOOL_PLAN_DISPATCH = "planDispatch";

    private String PROMPT_AUTO_AGENT;
    private static JsonSchemaResponseFormat reflectResponseFormat;

    public Map<String, AgentRuntimeInfo> getAllAgent(Agent agent, List<String> datasetIds) {
        Map<String, AgentRuntimeInfo> allAgentMap = new HashMap<>();
        getAllAgent(agent, datasetIds, new ArrayList<>(), allAgentMap);
        return allAgentMap;
    }

    /**
     * 获取所有agent,并判断agent调度链是否成环
     */
    private void getAllAgent(Agent agent, List<String> datasetIds, List<String> chain, Map<String, AgentRuntimeInfo> allAgentMap) {
        if (chain.contains(agent.getId())) {
            String text = chain.stream().map(i -> allAgentMap.get(i).agent.getName()).collect(Collectors.joining(" -> "))
                    + " -> " + agent.getName();
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "agent调度链成环:" + text);
        } else {
            try {
                chain.add(agent.getId());
                allAgentMap.put(agent.getId(), new AgentRuntimeInfo(agent, datasetIds));
                if (ObjectUtil.isNotEmpty(agent.getSubAgentIds())) {
                    List<Agent> subAgentList;
                    if (agent instanceof LocalAgent) {
                        subAgentList = localAgentService.getByIds(agent.getSubAgentIds()).stream().map(i -> (Agent) i).toList();
                    } else {
                        subAgentList = agentService.getByIds(agent.getSubAgentIds());
                    }
                    for (Agent subAgent : subAgentList) {
                        List<String> subDatasetIds = agentDatasetRelaService.listDatasets(subAgent.getId())
                                .parallelStream().map(Dataset::getId).toList();
                        getAllAgent(subAgent, subDatasetIds, chain, allAgentMap);
                    }
                }
            } finally {
                // 回溯
                chain.remove(agent.getId());
            }
        }
    }

    public Map<String, AgentExecutionSpec> create(Agent agent, List<String> datasetIds) {
        Map<String, AgentRuntimeInfo> allAgent = getAllAgent(agent, datasetIds);
        Map<String, AgentExecutionSpec> allAgentSpec = new HashMap<>();
        createInternal(allAgent, allAgent.get(agent.getId()), allAgentSpec);
        return allAgentSpec;
    }

    private void createInternal(Map<String, AgentRuntimeInfo> allAgent, AgentRuntimeInfo agentRuntimeInfo, Map<String, AgentExecutionSpec> allAgentSpec) {
        Agent agent = agentRuntimeInfo.agent;
        if (StrUtil.isEmpty(agent.getLlmModelId())) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }

        //model
        LlmModel model = agent instanceof LocalAgent ? localModelService.getById(agent.getLlmModelId())
                : modelService.findById(agent.getLlmModelId());
        if (model == null) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE, "Agent:" + agent.getName());
        }
        //检查模型可用性
        modelService.checkModelAvailable(model.getId(), agent.getId());

        if (Boolean.TRUE.equals(agent.getAutoAgentFlag())) {
            long count = modelService.lambdaQuery()
                    .eq(LlmModel::getWorkspaceId, agent.getWorkspaceId())
                    .eq(LlmModel::getType, "LLM")
                    .eq(LlmModel::getAutoAgent, true).count();
            if (count == 0) {
                throw new ServiceException(ServiceExceptionEnum.AUTO_AGENT_WITHOUT_MODEL_TO_USE);
            }
            //auto agent 内置提示词
            agent.setPrompt(getAutoAgentPrompt());
        }

        ChatModelRequest request = new ChatModelRequest();
        request.setLlmModelId(model.getId());
        request.setBaseUrl(model.getBaseUrl());
        request.setApiKey(model.getApiKey());
        request.setModel(model.getName());
        request.setTemperature(agent.getTemperature());
        request.setTopP(agent.getTopP());
        request.setMaxCompletionTokens(agent.getMaxTokens());
        request.setContextId(IdUtil.getSnowflakeNextIdStr());
        request.setTurns(agent.getTurns());

        if (ObjectUtil.equal(agent.getType(), AgentType.REFLECTION.getType())) {
            //反思agent定义response format
            request.setResponseFormat(buildResponseFormat());
        }

        if (StrUtil.isNotBlank(agent.getPrompt())) {
            //todo 移到外面cache data处
            chatContext.add(request.getContextId(), List.of(DeveloperMessage.of(agent.getPrompt())));
        }

        //tool
        List<ToolSpecification> tools = buildTools(agentRuntimeInfo, allAgent);
        if (!tools.isEmpty()) {
            //兼容deekseek,tool为空时传null
            request.setTools(tools);
        }

        AgentExecutionSpec param = new AgentExecutionSpec();
        param.setAgentId(agent.getId());
        param.setAgentName(agent.getName());
        param.setAgentType(agent.getType());
        param.setExecuteMode(agent.getMode());
        param.setDatasetIds(agentRuntimeInfo.datasetIds);
        param.setRequest(request);
        param.setVision(model.getVision());
        if (ObjectUtil.isNotEmpty(agent.getFunctionList())) {
            Map<String, Integer> functionExecuteMode = agent.getFunctionList().stream()
                    .collect(Collectors.toMap(Agent.AgentFunction::getFunctionId, Agent.AgentFunction::getMode));
            param.setFunctionExecuteMode(functionExecuteMode);
        }

        allAgentSpec.put(agent.getId(), param);

        //构建子agent
        if (ObjectUtil.isNotEmpty(agent.getSubAgentIds())) {
            List<String> reflectAgentIds = new ArrayList<>();
            for (String subAgentId : agent.getSubAgentIds()) {
                AgentRuntimeInfo subAgentRuntimeInfo = allAgent.get(subAgentId);
                if (subAgentRuntimeInfo == null) {
                    continue;
                }
                if (ObjectUtil.equal(subAgentRuntimeInfo.agent.getType(), AgentType.REFLECTION.getType())) {
                    //子agent为反思agent
                    reflectAgentIds.add(subAgentId);
                }
                createInternal(allAgent, subAgentRuntimeInfo, allAgentSpec);
            }
            param.setReflectAgentIds(reflectAgentIds);
        }
    }

    private List<ToolSpecification> buildTools(AgentRuntimeInfo agentRuntimeInfo, Map<String, AgentRuntimeInfo> allAgent) {
        List<ToolSpecification> toolList = new ArrayList<>();
        Agent agent = agentRuntimeInfo.agent;
        boolean localFlag = agent instanceof LocalAgent;

        if (ObjectUtil.isNotEmpty(agent.getFunctionList())) {
            List<String> functionIds = agent.getFunctionList().stream().map(Agent.AgentFunction::getFunctionId).toList();
            List<ToolFunction> functions = localFlag
                    ? localFunctionService.getByIds(functionIds).stream().map(i -> (ToolFunction) i).toList()
                    : toolFunctionService.getByIds(functionIds);

            for (ToolFunction function : functions) {
                ToolSpecification.Function f = new ToolSpecification.Function();
                f.setName(generateFunctionName(function.getResource(), function.getId()));
                f.setDescription(function.getDescription());

                if (ObjectUtil.isNotEmpty(function.getParameters())) {
                    JsonObjectSchema obj = new JsonObjectSchema();
                    Map<String, JsonSchemaElement> properties = new HashMap<>();
                    List<String> required = new ArrayList<>();
                    function.getParameters().forEach(param -> properties.put(param.getParamName(), toSchema(param, required)));
                    obj.setProperties(properties);
                    obj.setRequired(required);
                    f.setParameters(obj);
                }

                ToolSpecification specification = new ToolSpecification();
                specification.setFunction(f);
                toolList.add(specification);
            }
        }

        if (ObjectUtil.isNotEmpty(agentRuntimeInfo.datasetIds)) {
            //知识库
            toolList.add(buildKnowledgeTool());
        }
        if (ObjectUtil.isNotEmpty(agent.getSubAgentIds())) {
            //设置子agent调度工具
            List<Agent> subAgentList = agent.getSubAgentIds().stream()
                    .filter(subAgentId -> ObjectUtil.notEqual(allAgent.get(subAgentId).agent().getType(), AgentType.REFLECTION.getType()))
                    .map(subAgentId -> allAgent.get(subAgentId).agent()).toList();
            if (!subAgentList.isEmpty()) {
                toolList.add(buildDistributeTool(subAgentList));
            }
        }
        if (Boolean.TRUE.equals(agent.getAutoAgentFlag())) {
            //auto agent
            toolList.add(buildAutoAgentTool());
            toolList.add(buildPlanDispatchTool());
        }

        return toolList;
    }

    /**
     * 将工具转换为OpenAI jsonschema 格式
     */
    private JsonSchemaElement toSchema(ToolFunction.ParameterInfo param, List<String> required) {
        if (param.isRequired() && required != null) {
            required.add(param.getParamName());
        }
        return switch (param.getType()) {
            case "int", "integer" -> {
                JsonIntegerSchema schema = new JsonIntegerSchema();
                schema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    schema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    schema.setDefaultValue(param.getDefaultValue());
                }
                yield schema;
            }
            case "string", "String" -> {
                JsonStringSchema schema = new JsonStringSchema();
                schema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    schema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    schema.setDefaultValue(param.getDefaultValue());
                }
                yield schema;
            }
            case "bool", "boolean" -> {
                JsonBooleanSchema schema = new JsonBooleanSchema();
                schema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    schema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    schema.setDefaultValue(param.getDefaultValue());
                }
                yield schema;
            }
            case "object", "Object" -> {
                List<String> subRequired = new ArrayList<>();
                Map<String, JsonSchemaElement> properties = new HashMap<>();
                param.getProperties().forEach(p -> properties.put(p.getParamName(), toSchema(p, subRequired)));
                JsonObjectSchema schema = new JsonObjectSchema();
                schema.setProperties(properties);
                schema.setRequired(subRequired);
                schema.setDescription(param.getDescription());
                yield schema;
            }
            case "array", "Array" -> {
                JsonArraySchema schema = new JsonArraySchema();
                schema.setDescription(param.getDescription());
                if (ObjectUtil.isNotEmpty(param.getProperties())) {
                    schema.setItems(toSchema(param.getProperties().get(0), null));
                }
                yield schema;
            }
            default -> {
                JsonNumberSchema schema = new JsonNumberSchema();
                schema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    schema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    schema.setDefaultValue(param.getDefaultValue());
                }
                yield schema;
            }
        };
    }

    private String generateFunctionName(String name, String functionId) {
        //生成传给大模型的function名字: functionName+dbId组成
        return name.replace("/", "").replace(" ", "") + "_" + functionId;
    }

    public static String getFunctionId(String functionName) {
        String[] arr = functionName.split("_");
        if (arr.length < 2) {
            return null;
        }
        return arr[arr.length - 1];
    }

    /**
     * 将knowledge封装成tool供LLM调用
     */
    private ToolSpecification buildKnowledgeTool() {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_KNOWLEDGE_BASE);
        function.setDescription("retrieve content from knowledge base");

        JsonStringSchema content = new JsonStringSchema();
        content.setDescription("query text for knowledge base");

        JsonObjectSchema obj = new JsonObjectSchema();
        obj.setProperties(Map.of("content", content));
        obj.setRequired(List.of("content"));
        function.setParameters(obj);

        ToolSpecification spec = new ToolSpecification();
        spec.setFunction(function);
        return spec;
    }

    /**
     * 子agent封装成工具
     */
    private ToolSpecification buildDistributeTool(List<Agent> agentList) {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_AGENT_DISTRIBUTE);

        List<Dict> list = agentList.stream().map(i -> Dict.create()
                .set("agentId", i.getId())
                .set("agentName", i.getName())
                .set("description", i.getDescription())).toList();
        function.setDescription("dispatch command to agent, candidates: " + JSONUtil.toJsonStr(list));

        //cmd
        JsonStringSchema cmd = new JsonStringSchema();
        cmd.setDescription("command for target agent");

        //agentId
        JsonStringSchema agentId = new JsonStringSchema();
        agentId.setDescription("target agent id");
        List<Object> agentIds = agentList.stream().map(i -> (Object) i.getId()).toList();
        agentId.setEnums(agentIds);

        //agentName
        JsonStringSchema agentName = new JsonStringSchema();
        agentName.setDescription("target agent name");
        agentName.setEnums(agentList.stream().map(i -> (Object) i.getName()).toList());

        //imageUrls
        JsonArraySchema imageUrls = new JsonArraySchema();
        imageUrls.setDescription("image urls for vision recognition");
        JsonStringSchema imageUrlItem = new JsonStringSchema();
        imageUrlItem.setDescription("image url");
        imageUrls.setItems(imageUrlItem);

        //videoUrl
        JsonStringSchema videoUrl = new JsonStringSchema();
        videoUrl.setDescription("video url for vision recognition");

        JsonObjectSchema obj = new JsonObjectSchema();
        Map<String, JsonSchemaElement> properties = new HashMap<>();
        properties.put("cmd", cmd);
        properties.put("agentId", agentId);
        properties.put("agentName", agentName);
        properties.put("imageUrls", imageUrls);
        properties.put("videoUrl", videoUrl);
        obj.setProperties(properties);
        obj.setRequired(List.of("cmd", "agentId", "agentName"));
        function.setParameters(obj);

        ToolSpecification spec = new ToolSpecification();
        spec.setFunction(function);
        return spec;
    }

    /**
     * 让大模型通过function-calling方式触发auto-agent功能
     */
    private ToolSpecification buildAutoAgentTool() {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_AUTO_AGENT);
        function.setDescription("Delegate the request to the dedicated PlanningAgent.");

        JsonStringSchema task = new JsonStringSchema();
        task.setDescription("The original user request. Keep the user's language and preserve key constraints, scope, expected output, and missing context.");

        JsonObjectSchema obj = new JsonObjectSchema();
        obj.setProperties(Map.of("task", task));
        obj.setRequired(List.of("task"));
        function.setParameters(obj);

        ToolSpecification spec = new ToolSpecification();
        spec.setFunction(function);
        return spec;
    }

    /**
     * 计划节点调度工具
     */
    private ToolSpecification buildPlanDispatchTool() {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_PLAN_DISPATCH);
        function.setDescription("Dispatch the next action for an existing plan result in context.");

        JsonStringSchema action = new JsonStringSchema();
        action.setDescription("The dispatch action.");
        action.setEnums(List.of("EXECUTE_NODE", "EXECUTE_BATCH", "FINISH"));

        JsonStringSchema planId = new JsonStringSchema();
        planId.setDescription("The plan id from the plan result in context.");

        JsonStringSchema agentId = new JsonStringSchema();
        agentId.setDescription("Plan agent id.");

        JsonStringSchema input = new JsonStringSchema();
        input.setDescription("The current task input for that plan agent.");

        JsonObjectSchema taskObj = new JsonObjectSchema();
        taskObj.setProperties(Map.of("agentId", agentId, "input", input));
        taskObj.setRequired(List.of("agentId", "input"));

        JsonArraySchema tasks = new JsonArraySchema();
        tasks.setDescription("Plan agent tasks to execute. Ignored when action is FINISH.");
        tasks.setItems(taskObj);

        JsonStringSchema reason = new JsonStringSchema();
        reason.setDescription("Optional short reason.");

        JsonObjectSchema obj = new JsonObjectSchema();
        Map<String, JsonSchemaElement> properties = new HashMap<>();
        properties.put("action", action);
        properties.put("planId", planId);
        properties.put("tasks", tasks);
        properties.put("reason", reason);
        obj.setProperties(properties);
        obj.setRequired(List.of("action", "planId", "tasks"));
        function.setParameters(obj);

        ToolSpecification spec = new ToolSpecification();
        spec.setFunction(function);
        return spec;
    }

    /**
     * 获取反思的response-format
     */
    public static JsonSchemaResponseFormat buildResponseFormat() {
        if (reflectResponseFormat == null) {
            Map<String, JsonSchemaElement> properties = new HashMap<>();
            //information
            JsonStringSchema information = new JsonStringSchema();
            information.setDescription("Concise summary of the reflection result");
            properties.put("information", information);

            //score
            JsonIntegerSchema score = new JsonIntegerSchema();
            score.setDescription("Evaluation score for the result, ranging from 0 to 10; a reflection passes only when the score is greater than 7");
            properties.put("score", score);

            JsonObjectSchema obj = new JsonObjectSchema();
            obj.setProperties(properties);
            obj.setRequired(List.of("information", "score"));

            JsonSchemaResponseFormat.JsonSchema jsonSchema = new JsonSchemaResponseFormat.JsonSchema();
            jsonSchema.setSchema(obj);
            jsonSchema.setName("response");
            jsonSchema.setDescription("the reflection result passes only when the score is greater than 7");

            reflectResponseFormat = new JsonSchemaResponseFormat();
            reflectResponseFormat.setJsonSchema(jsonSchema);
        }
        return reflectResponseFormat;
    }

    private String getAutoAgentPrompt() {
        if (StrUtil.isEmpty(PROMPT_AUTO_AGENT)) {
            PROMPT_AUTO_AGENT = ResourceUtil.readUtf8Str("classpath:prompt/auto-agent.txt");
        }
        return PROMPT_AUTO_AGENT;
    }

    public record AgentRuntimeInfo(Agent agent, List<String> datasetIds) {
    }
}
