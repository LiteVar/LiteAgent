package com.litevar.agent.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 本地agent信息
 *
 * @author uncle
 * @since 2024/11/14 14:43
 */
@Data
public class LocalAgentInfoDTO {
    /**
     * agentId
     */
    @NotBlank
    private String id;

    /**
     * agent名称
     */
    @NotBlank
    private String name;
    /**
     * 描述
     */
    private String description = "";

    /**
     * 提示词
     */
    private String prompt = "";

    /**
     * 关联的大模型id
     */
    private String llmModelId = "";
    /**
     * 关联的工具列表
     */
    @Deprecated
    private List<String> toolIds = Collections.emptyList();

    /**
     * 温度值
     */
    private Double temperature;

    /**
     * 概率抽样的 p 值
     */
    private Double topP;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;
    /**
     * 方法列表
     */
    private List<FunctionVO> toolFunctionList;
    /**
     * 子agent(普通,分发,分思) id
     */
    private List<String> subAgentIds;
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
     * 知识库id
     */
    private List<String> datasetIds;

    /**
     * 创建时间
     */
    @NotNull
    private LocalDateTime createTime;

    @Data
    public static class FunctionVO {
        /**
         * 工具id
         */
        private String toolId;
        /**
         * 方法名字
         */
        private String functionName;
        /**
         * 请求方式
         */
        private String requestMethod;
        /**
         * 执行模式
         *
         * @see com.litevar.agent.base.enums.ExecuteMode
         */
        private Integer mode;
    }
}
