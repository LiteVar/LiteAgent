package com.litevar.agent.core.module.tool.parser;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import com.litevar.agent.core.util.McpUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author reid
 * @since 2025/5/19
 */

@Component
public class ToolMcpParser implements ToolParser, InitializingBean {

    @Override
    public List<ToolFunction> parse(String rawStr) {
        JSONObject mcp = JSONUtil.parseObj(rawStr);
        String name = mcp.getStr("name");
        String baseUrl = mcp.getStr("baseUrl");
        String sseEndpoint = mcp.getStr("sseEndpoint");
        if (StrUtil.hasEmpty(name, baseUrl, sseEndpoint)) {
            throw new ServiceException(1000, "schema参数name,baseUrl,sseEndpoint都不能为空");
        }

        List<McpSchema.Tool> tools;
        try {
            McpSyncClient mcpClient = McpUtil.getSyncClient(name, baseUrl, sseEndpoint);
            McpSchema.ListToolsResult toolsResult = mcpClient.listTools();
            tools = toolsResult.tools();
        } catch (Exception ex) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "Mcp server不可用");
        }

        if (tools == null || tools.isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "Mcp server returns no tools.");
        }

        List<ToolFunction> functions = new ArrayList<>(tools.size());
        tools.forEach(tool -> {
            ToolFunction function = new ToolFunction();
            function.setResource(tool.name());
            function.setDescription(tool.description());
            function.setProtocol(FunctionExecutor.MCP);
            function.setServerUrl(mcp.getStr("baseUrl"));
            function.setRequestMethod("POST");
            function.setContentType("application/json");

            // 解析输入参数
            McpSchema.JsonSchema jsonSchema = tool.inputSchema();
            if (jsonSchema != null) {
                if ("object".equals(jsonSchema.type()) && jsonSchema.properties() != null) {
                    // 遍历属性
                    jsonSchema.properties().forEach((paramName, paramSchema) -> {
                        JSONObject paramJson = JSONUtil.parseObj(paramSchema);
                        ToolFunction.ParameterInfo paramInfo = parseParameter(paramName, paramJson);
                        // 检查是否必需
                        if (jsonSchema.required() != null) {
                            paramInfo.setRequired(jsonSchema.required().contains(paramName));
                        }
                        paramInfo.setIn(FunctionExecutor.BODY);
                        function.getParameters().add(paramInfo);
                    });
                }
            }

            functions.add(function);
        });

        return functions;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.MCP, this);
    }

    private ToolFunction.ParameterInfo parseParameter(String paramName, JSONObject paramJson) {
        ToolFunction.ParameterInfo paramInfo = new ToolFunction.ParameterInfo();
        paramInfo.setParamName(paramName);
        paramInfo.setType(paramJson.getStr("type"));
        paramInfo.setDescription(paramJson.getStr("description"));

        // 处理数组类型
        if ("array".equals(paramJson.getStr("type"))) {
            JSONObject items = paramJson.getJSONObject("items");
            if (items != null) {
                // 递归解析数组元素
                ToolFunction.ParameterInfo itemInfo = parseParameter("items", items);
                paramInfo.getProperties().add(itemInfo);
            }
        }

        // 处理对象类型
        if ("object".equals(paramJson.getStr("type"))) {
            JSONObject properties = paramJson.getJSONObject("properties");
            if (properties != null) {
                properties.forEach((propName, propValue) -> {
                    ToolFunction.ParameterInfo propInfo = parseParameter(propName, JSONUtil.parseObj(propValue));
                    paramInfo.getProperties().add(propInfo);
                });
            }
        }

        return paramInfo;
    }
}
