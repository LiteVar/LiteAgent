package com.litevar.agent.base.vo;

import lombok.Data;

/**
 * @author uncle
 * @since 2025/3/17 15:58
 */
@Data
public class FunctionVO {
    private String toolId;
    private String toolName;
    private String functionId;
    private String functionName;
    private String functionDesc;
    private String protocol;
    private String icon;
    /**
     * @see com.litevar.agent.base.enums.ExecuteMode
     */
    private Integer mode;

    private String requestMethod;
}