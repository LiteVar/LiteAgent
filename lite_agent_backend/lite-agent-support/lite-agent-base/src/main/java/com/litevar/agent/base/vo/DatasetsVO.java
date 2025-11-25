package com.litevar.agent.base.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * View Object for Dataset with additional statistics.
 */
@Data
public class DatasetsVO {
    private String id;
    private String name;
    private String userId;
    private String workspaceId;
    private String icon;
    private String description;
    private Boolean shareFlag;
    /**
     * 文本模型id
     */
    private String llmModelId;
    private String embeddingModel;
    private String embeddingModelProvider;
    private Integer retrievalTopK;
    private Double retrievalScoreThreshold;
    private String apiUrl;
    private String apiKey;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Integer docCount = 0;
    private Integer wordCount = 0;
    private Integer agentCount = 0;
    private Boolean canEdit = Boolean.FALSE;
    private Boolean canDelete = Boolean.FALSE;
}