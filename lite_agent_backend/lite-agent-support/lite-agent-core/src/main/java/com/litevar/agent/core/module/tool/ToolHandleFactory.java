package com.litevar.agent.core.module.tool;

import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import com.litevar.agent.core.module.tool.parser.ToolParser;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author reid
 * @since 2024/8/5
 */
public class ToolHandleFactory {

    private static final Map<ToolSchemaType, ToolParser> PARSE_INSTANCE = new ConcurrentHashMap<>();
    private static final Map<String, FunctionExecutor> FUNCTION_EXECUTOR = new ConcurrentHashMap<>();

    public static ToolParser getParseInstance(ToolSchemaType type) {
        return PARSE_INSTANCE.get(type);
    }

    public static void registerParser(ToolSchemaType type, ToolParser parser) {
        Assert.notNull(type, "type must not be null");
        PARSE_INSTANCE.put(type, parser);
    }


    public static FunctionExecutor getFunctionExecutor(String type) {
        return FUNCTION_EXECUTOR.get(type);
    }

    public static void registerFunctionExecutor(String type, FunctionExecutor functionExecutor) {
        Assert.notNull(type, "type must not be null");
        FUNCTION_EXECUTOR.put(type, functionExecutor);
    }
}
