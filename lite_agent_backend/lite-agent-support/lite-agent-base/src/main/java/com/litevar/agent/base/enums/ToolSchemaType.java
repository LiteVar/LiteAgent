package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 工具类型枚举
 *
 * @author reid
 * @since 2024/8/5
 */
@Getter
@AllArgsConstructor
public enum ToolSchemaType {
    /**
     * openapi
     */
    OPEN_API3(1, "openapi"),
    /**
     * json rpc
     */
    JSON_RPC(2, "jsonrpc"),
    /**
     * open modbus
     */
    OPEN_MODBUS(3, "openmodbus"),
    /**
     * open tool
     */
    OPEN_TOOL(4, "opentool"),
    /**
     * mcp
     */
    MCP(5, "mcp");

    private final Integer value;
    private final String name;

    public static ToolSchemaType of(Integer value) {
        ToolSchemaType res = null;
        for (ToolSchemaType type : values()) {
            if (Objects.equals(value, type.value)) {
                res = type;
                break;
            }
        }
        return res;
    }
}
