package com.litevar.agent.core.module.tool.executor;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.core.util.McpUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author reid
 * @since 2025/5/19
 */

@Slf4j
@Component
public class McpFunctionExecutor implements FunctionExecutor, InitializingBean {
    @Autowired
    private ToolService toolService;

    @SneakyThrows
    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        ToolProvider mcp = toolService.findById(info.getToolId());
        if (mcp == null) {
            throw new ServiceException("[mcp调用]函数不存在:" + info.getToolId());
        }

        JSONObject jsonObject = JSONUtil.parseObj(mcp.getSchemaStr());
        McpSyncClient mcpClient = McpUtil.getSyncClient(jsonObject.getStr("name"), jsonObject.getStr("baseUrl"), jsonObject.getStr("sseEndpoint"));
        McpSchema.CallToolResult result = mcpClient.callTool(new McpSchema.CallToolRequest(info.getResource(), data));

        if (result.isError()) {
            throw new ServiceException("[mcp调用]调用失败:" + info.getToolId() + "," + result.content());
        }

        return new ObjectMapper().writeValueAsString(result.content());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(MCP, this);
    }
}
