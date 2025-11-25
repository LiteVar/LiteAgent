package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author reid
 * @since 2/19/25
 */

@Data
@CollectionName("dataset_document")
public class DatasetDocument {
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
    @MongoIndex
    private String datasetId;
    /**
     * 文档类型: INPUT, FILE, HTML
     */
    private String dataSourceType;
    /**
     * 文件id
     */
    private String fileId;
    /**
     * input content
     */
    private String content;
    /**
     * HTML URL
     */
    private List<String> htmlUrl;
    /**
     * MD5 hash of the document
     */
    private String md5Hash;
    /**
     * document word count
     */
    private Integer wordCount = 0;
    /**
     * document token count
     */
    private Integer tokenCount = 0;
    /**
     * default chunk size
     */
    private Integer chunkSize = 500;
    /**
     * default Separator to split the document.
     */
    private String separator = "\n\n";
    /**
     * Document metadata (JSON)
     */
    private String metadata = "";
    /**
     * 激活/冻结
     */
    private Boolean enableFlag = Boolean.TRUE;
    /**
     * 文档向量化状态: PENDING, SUCCESS, FAIL
     */
    private String embedStatus = "PENDING";

    /**
     * 是否需要做摘要
     */
    private Boolean needSummary = Boolean.TRUE;

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
