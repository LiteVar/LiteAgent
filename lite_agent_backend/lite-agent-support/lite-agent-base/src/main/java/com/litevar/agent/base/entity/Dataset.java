package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库
 *
 * @author reid
 * @since 2/19/25
 */

@Data
@CollectionName("dataset")
public class Dataset {
    /**
     * id
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 名称
     */
    private String name;
    /**
     * 创建者id
     */
    private String userId;
    /**
     * 工作空间id
     */
    @MongoIndex
    private String workspaceId;
    /**
     * 图标
     */
    private String icon;
    /**
     * 描述
     */
    private String description;
    /**
     * 分享标识
     */
    private Boolean shareFlag = Boolean.FALSE;
    /**
     * 数据源类型: INPUT, FILE, HTML
     */
    private String dataSourceType;
    /**
     * 向量库集合名称
     */
    private String vectorCollectionName;
    /**
     * 关联的大模型id
     */
    private String llmModelId;
    /**
     * embedding 模型
     */
    private String embeddingModel;
    /**
     * embedding 模型提供商
     */
    private String embeddingModelProvider;
    /**
     * 检索 TopK
     */
    private Integer retrievalTopK;
    /**
     * 检索分数阈值
     */
    private Double retrievalScoreThreshold;

    /**
     * 分享给外部的apiUrl
     */
    private String apiUrl;
    /**
     * 分享给外部的apikey
     */
    private String apiKey;

    /**
     * 创建时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @CollectionField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
