package com.litevar.agent.base.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @author reid
 * @since 2024/8/13
 */

@Data
public class AgentUpdateForm {
    /**
     * agent名称
     */
    @NotBlank
    private String name;
    /**
     * 图标url
     */
    private String icon;
    /**
     * 描述,可空
     */
    private String description;
    /**
     * 提示词
     */
    private String prompt;
    /**
     * 关联的大模型id
     */
    private String llmModelId;

    /**
     * 工具
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
