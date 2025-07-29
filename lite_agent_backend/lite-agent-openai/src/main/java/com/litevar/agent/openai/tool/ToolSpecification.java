package com.litevar.agent.openai.tool;

import lombok.Data;

/**
 * 工具
 *
 * @author uncle
 * @since 2025/2/21 10:38
 */
@Data
public class ToolSpecification {
    /**
     * The type of the tool. Currently, only function is supported.
     */
    private final String type = "function";
    private Function function;

    @Data
    public static class Function {
        /**
         * 函数名字
         */
        private String name;
        /**
         * 函数描述
         */
        private String description;
        /**
         * 函数参数
         */
        private JsonObjectSchema parameters;
    }
}
