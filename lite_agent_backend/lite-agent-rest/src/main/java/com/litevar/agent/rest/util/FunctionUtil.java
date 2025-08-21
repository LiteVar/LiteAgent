package com.litevar.agent.rest.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import com.litevar.agent.core.module.tool.executor.OpenToolExecutor;
import com.litevar.agent.openai.tool.*;
import com.litevar.agent.rest.openai.agent.AgentManager;
import com.litevar.agent.rest.openai.agent.AgentMsgType;
import com.litevar.agent.rest.openai.agent.MultiAgent;
import com.litevar.agent.rest.openai.executor.TaskExecutor;
import com.litevar.agent.rest.openai.message.OpenToolMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2025/2/27 14:50
 */
@Slf4j
public class FunctionUtil {
    public static final String TOOL_KNOWLEDGE_BASE = "knowledgeBase";
    public static final String TOOL_AGENT_DISTRIBUTE = "agentDistribute";
    public static final String TOOL_AUTO_AGENT = "task_plan";

    public static List<ToolSpecification> buildTool(List<ToolFunction> functionList) {
        List<ToolSpecification> invokeToolList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(functionList)) {
            functionList.forEach(dbFunction -> {
                //名字由functionName+dbId组成
                String name = generateFunctionName(dbFunction.getResource(), dbFunction.getId());

                ToolSpecification.Function function = new ToolSpecification.Function();
                function.setName(name);
                function.setDescription(dbFunction.getDescription());

                if (!dbFunction.getParameters().isEmpty()) {
                    JsonObjectSchema obj = new JsonObjectSchema();
                    List<String> required = new ArrayList<>();
                    Map<String, JsonSchemaElement> properties = new HashMap<>();
                    for (ToolFunction.ParameterInfo functionParam : dbFunction.getParameters()) {
                        JsonSchemaElement element = travelField(functionParam, required);
                        properties.put(functionParam.getParamName(), element);
                    }

                    obj.setProperties(properties);
                    obj.setRequired(required);
                    function.setParameters(obj);
                }

                ToolSpecification specification = new ToolSpecification();
                specification.setFunction(function);

                invokeToolList.add(specification);
            });
        }
        return invokeToolList;
    }

    private static JsonSchemaElement travelField(ToolFunction.ParameterInfo param, List<String> required) {
        if (param.isRequired() && required != null) {
            required.add(param.getParamName());
        }
        return switch (param.getType()) {
            case "int", "integer" -> {
                JsonIntegerSchema integerSchema = new JsonIntegerSchema();
                integerSchema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    integerSchema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    integerSchema.setDefaultValue(param.getDefaultValue());
                }
                yield integerSchema;
            }

            case "string", "String" -> {
                JsonStringSchema stringSchema = new JsonStringSchema();
                stringSchema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    stringSchema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    stringSchema.setDefaultValue(param.getDefaultValue());
                }
                yield stringSchema;
            }

            case "bool", "boolean" -> {
                JsonBooleanSchema booleanSchema = new JsonBooleanSchema();
                booleanSchema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    booleanSchema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    booleanSchema.setDefaultValue(param.getDefaultValue());
                }
                yield booleanSchema;
            }

            case "object", "Object" -> {
                List<String> subRequired = new ArrayList<>();
                Map<String, JsonSchemaElement> subProperties = new HashMap<>();
                for (ToolFunction.ParameterInfo subParam : param.getProperties()) {
                    JsonSchemaElement subElement = travelField(subParam, subRequired);
                    subProperties.put(subParam.getParamName(), subElement);
                }
                JsonObjectSchema objectSchema = new JsonObjectSchema();
                objectSchema.setProperties(subProperties);
                objectSchema.setDescription(param.getDescription());
                objectSchema.setRequired(subRequired);
                yield objectSchema;
            }

            case "array", "Array" -> {
                JsonArraySchema arraySchema = new JsonArraySchema();
                arraySchema.setDescription(param.getDescription());
                ToolFunction.ParameterInfo item = param.getProperties().get(0);
                JsonSchemaElement subElement = travelField(item, null);
                arraySchema.setItems(subElement);
                yield arraySchema;
            }

            default -> {
                JsonNumberSchema numberSchema = new JsonNumberSchema();
                numberSchema.setDescription(param.getDescription());
                if (!param.getEnums().isEmpty()) {
                    numberSchema.setEnums(param.getEnums());
                }
                if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
                    numberSchema.setDefaultValue(param.getDefaultValue());
                }
                yield numberSchema;
            }
        };
    }

    /**
     * function调用
     */
    public static String callFunction(String callId, String contextId, ToolFunction function, String argument) {
        Integer executeMode = (Integer) RedisUtil.getHashValue(String.format(CacheKey.FUNCTION_EXECUTE_MODE, contextId), function.getId());
        String apiKey = SpringUtil.getBean(ToolService.class).toolApiKey(function.getToolId());
        CurrentAgentRequest.AgentRequest context = CurrentAgentRequest.getContext();
        Callable<Object> task = () -> {
            JSONObject argObj = JSONUtil.parseObj(argument);
            //调用接口获取结果
            FunctionExecutor executor = ToolHandleFactory.getFunctionExecutor(function.getProtocol());
            Map<String, String> defineHeader = new HashMap<>();
            if (StrUtil.isNotEmpty(apiKey)) {
                defineHeader.put(HttpHeaders.AUTHORIZATION, apiKey);
            }
            String result;
            if (executor instanceof OpenToolExecutor) {
                //推一条FunctionCall消息给第三方, 让第三方去调用工具
                OpenToolMessage msg = new OpenToolMessage(context, callId, function.getResource(), argObj);
                AgentManager.handleMsg(AgentMsgType.OPEN_TOOL_CALL_MSG, msg);
            }
            try {
                result = executor.invoke(callId, function, argObj, defineHeader);
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error(ex.getMessage());
                result = "调用失败";
            }
            return result;
        };
        try {
            //id使用agentId+functionId表示执行模式控制范围是在同一个agent的同一个function有效
            CompletableFuture<Object> future = TaskExecutor.execute(context.getAgentId() + function.getId(), executeMode, task);
            return (String) future.join();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 生成传给大模型的function名字
     *
     * @param name       原来的名字
     * @param functionId
     * @return
     */
    public static String generateFunctionName(String name, String functionId) {
        //名字由functionName+dbId组成
        return name.replace("/", "")
                .replace(" ", "") + "_" + functionId;
    }

    /**
     * 根据functionName得到functionId
     */
    public static String getFunctionId(String name) {
        String[] arr = name.split("_");
        return arr[arr.length - 1];
    }

    /**
     * 将agent封装成tool供LLM调用
     * {"cmd": xxx, "agentId": 123, "agentName": abc}
     */
    public static ToolSpecification agentToTool(Collection<MultiAgent> agentList) {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_AGENT_DISTRIBUTE);
        function.setDescription("agent command distribute");

        JsonStringSchema cmd = new JsonStringSchema();
        cmd.setDescription("调用agent时传递的内容");

        JsonStringSchema agentId = new JsonStringSchema();
        String agentIdAndName = agentList.stream().map(i -> i.getAgentName().trim() + "(" + i.getAgentId() + ")")
                .collect(Collectors.joining(" | "));
        agentId.setDescription("agentId " + agentIdAndName);
        List<Object> agentIds = agentList.stream().map(i -> (Object) i.getAgentId()).toList();
        agentId.setEnums(agentIds);

        JsonStringSchema agentName = new JsonStringSchema();
        agentName.setDescription("agent name");
        agentName.setEnums(agentList.stream().map(i -> (Object) i.getAgentName()).toList());

        JsonObjectSchema obj = new JsonObjectSchema();
        obj.setProperties(Map.of("cmd", cmd, "agentId", agentId, "agentName", agentName));
        obj.setRequired(List.of("cmd", "agentId", "agentName"));
        function.setParameters(obj);

        ToolSpecification specification = new ToolSpecification();
        specification.setFunction(function);
        return specification;
    }

    /**
     * 将knowledge封装成tool供LLM调用
     */
    public static ToolSpecification knowledgeToTool() {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_KNOWLEDGE_BASE);
        function.setDescription("search knowledge base of the company");

        JsonStringSchema content = new JsonStringSchema();
        content.setDescription("search content");

        JsonObjectSchema obj = new JsonObjectSchema();
        obj.setProperties(Map.of("content", content));
        obj.setRequired(List.of("content"));
        function.setParameters(obj);

        ToolSpecification specification = new ToolSpecification();
        specification.setFunction(function);
        return specification;
    }

    /**
     * 让大模型通过function-calling方式触发auto-agent
     */
    public static ToolSpecification autoAgentToTool() {
        ToolSpecification.Function function = new ToolSpecification.Function();
        function.setName(TOOL_AUTO_AGENT);
        function.setDescription("processing user tasks");
        JsonStringSchema task = new JsonStringSchema();
        task.setDescription("the task content, which can be a single task or multiple tasks.When multiple tasks are present,separate them with commas.Output must always match the user's language.");

        JsonObjectSchema obj = new JsonObjectSchema();
        obj.setProperties(Map.of("task", task));
        obj.setRequired(List.of("task"));
        function.setParameters(obj);
        ToolSpecification specification = new ToolSpecification();
        specification.setFunction(function);
        return specification;
    }

}
