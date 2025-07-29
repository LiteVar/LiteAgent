package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.annotation.index.MongoTextIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author reid
 * @since 2/19/25
 */

@Data
@CollectionName("document_segment")
@MongoTextIndex(fields = {"content"})
public class DocumentSegment {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String userId;
    /**
     * 工作空间id
     */
    @MongoIndex
    private String workspaceId;
    /**
     * 知识库id
     */
    @MongoIndex
    private String datasetId;
    /**
     * 文档id
     */
    @MongoIndex
    private String documentId;
    /**
     * 向量数据库里的对应segment id
     */
    @MongoIndex
    private String embeddingId;
    /**
     * 向量库集合名称
     */
    private String vectorCollectionName;
    /**
     * 片段文本内容
     */
    private String content;
    /**
     * 片段元数据
     */
    private String metadata = "";
    /**
     * 字数
     */
    private Integer wordCount = 0;
    /**
     * token数
     */
    private Integer tokenCount = 0;
    /**
     * 检索命中次数
     */
    private Integer hitCount = 0;
    /**
     * 是否启用
     */
    private Boolean enableFlag = Boolean.TRUE;
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
