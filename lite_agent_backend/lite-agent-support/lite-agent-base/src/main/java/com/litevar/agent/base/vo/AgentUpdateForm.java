package com.litevar.agent.base.vo;

import com.litevar.agent.base.entity.Agent;
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

    /**
     * agent类型
     *
     * @see com.litevar.agent.base.enums.AgentType
     */
    private Integer type = 0;

    /**
     * 执行模式
     *
     * @see com.litevar.agent.base.enums.ExecuteMode
     */
    private Integer mode = 0;

    /**
     * 子agent(普通,分发,分思) id
     */
    private List<String> subAgentIds;

    /**
     * 方法列表
     */
    private List<Agent.AgentFunction> functionList;

    /**
     * 方法执行顺序(functionId)
     */
    private List<String> sequence;

    /**
     * 数据集id
     */
    private List<String> datasetIds;

    /**
     * tts模型id
     */
    private String ttsModelId;

    /**
     * asr模型id
     */
    private String asrModelId;
}
