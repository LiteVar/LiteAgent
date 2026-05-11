package com.litevar.agent.core.module.tool.executor;

import com.litevar.agent.base.entity.ToolFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author uncle
 * @since 2024/10/18 12:02
 */
public interface FunctionExecutor {
    String HTTP = "http";
    String JSON_RPC = "jsonRpc";
    String MODBUS = "modbus";
    String EXTERNAL = "external";
    String MCP = "mcp";
    String OPEN_TOOL = "openTool";
    String STREAM_DONE = "----DONE----";

    String HEADER = "header";
    String BODY = "body";
    String QUERY = "query";
    String PATH = "path";

    /**
     * 接口调用
     *
     * @param info         接口信息
     * @param data         参数数据(body,query,header,path参数)
     * @param defineHeader tool定义的header数据
     * @return
     */
    String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader);

    /**
     * 工具stream式调用
     *
     * @param callId
     * @param info
     * @param data
     * @param onStream 回调结果给agent处理
     */
    default void streamCall(String callId, ToolFunction info, Map<String, Object> data,
                            Consumer<List<String>> onStream) {
        throw new UnsupportedOperationException("streamCall not supported");
    }
}
