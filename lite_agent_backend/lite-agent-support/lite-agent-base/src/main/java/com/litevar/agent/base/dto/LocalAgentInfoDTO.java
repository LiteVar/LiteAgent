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
     * 创建时间
     */
    @NotNull
    private LocalDateTime createTime;
}
