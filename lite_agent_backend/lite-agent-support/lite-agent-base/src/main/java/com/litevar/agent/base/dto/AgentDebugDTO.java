package com.litevar.agent.base.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * agent调试初始化会话参数
 *
 * @author uncle
 * @since 2024/8/29 15:28
 */
@Data
public class AgentDebugDTO {
    /**
     * agent id
     */
    @NotBlank
    private String agentId;
    /**
     * 模型id
     */
    @NotBlank
    private String modelId;
    /**
     * 提示词
     */
    private String prompt;
    /**
     * 工具id
     */
    private List<String> toolIds;

    /**
     * 温度值
     */
    @Min(value = 0)
    @Max(value = 1)
    private Double temperature;

    /**
     * 概率抽样的 p 值
     */
    @Min(value = 0)
    @Max(value = 1)
    private Double topP;

    /**
     * 最大 token 数
     */
    @Min(value = 1)
    private Integer maxTokens;
}
