package com.litevar.agent.base.dto;

import com.litevar.agent.base.entity.Agent;
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
    @Deprecated
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

    /**
     * 子agent(普通,分发,分思) id
     */
    private List<String> subAgentIds;

    /**
     * agent执行模式
     *
     * @see com.litevar.agent.base.enums.ExecuteMode
     */
    private Integer mode = 0;

    /**
     * agent类型
     *
     * @see com.litevar.agent.base.enums.AgentType
     */
    private Integer type;

    /**
     * 方法列表
     */
    private List<Agent.AgentFunction> functionList;

    /**
     * 数据集id
     */
    private List<String> datasetIds;

    /**
     * 方法执行顺序(functionId)
     */
    private List<String> sequence;
}
