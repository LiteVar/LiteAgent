package com.litevar.agent.base.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 插件访问统计请求
 *
 * @author uncle
 * @since 2026/01/14 12:13
 */
@Data
public class PluginAnalyzeVO {
    /**
     * 智连ID
     */
    @NotBlank
    private String connectorId;

    /**
     * 开始时间
     */
    @NotBlank
    private String startTime;

    /**
     * 结束时间
     */
    @NotBlank
    private String endTime;
}
