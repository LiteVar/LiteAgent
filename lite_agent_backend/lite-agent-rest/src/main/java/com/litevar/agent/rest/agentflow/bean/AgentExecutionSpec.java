package com.litevar.agent.rest.agentflow.bean;

import com.litevar.agent.openai.completion.ChatModelRequest;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * agent运行时参数
 *
 * @author uncle
 * @since 2025/12/16 18:07
 */
@Data
public class AgentExecutionSpec {
    private String agentId;
    private String agentName;
    /**
     * agent类型
     *
     * @see com.litevar.agent.base.enums.AgentType
     */
    private Integer agentType;
    /**
     * 执行模式
     *
     * @see com.litevar.agent.base.enums.ExecuteMode
     */
    private Integer executeMode;
    private List<String> datasetIds;
    private ChatModelRequest request;
    /**
     * 工具执行模式(functionId,executeMode)
     */
    private Map<String, Integer> functionExecuteMode;

    /**
     * 反思的agent id
     */
    private List<String> reflectAgentIds;
    /**
     * 普通模式的子agent id
     */
    @Deprecated
    private List<String> subAgentIds;
    /**
     * 是否支持视觉识别(VL模型)
     */
    private Boolean vision;
}
