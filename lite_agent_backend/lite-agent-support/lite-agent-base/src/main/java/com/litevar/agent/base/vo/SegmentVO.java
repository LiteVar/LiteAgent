package com.litevar.agent.base.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author reid
 * @since 3/27/25
 */

@Data
public class SegmentVO {
    private String id;
    private String userId;
    /**
     * 工作空间id
     */
    private String workspaceId;
    /**
     * 知识库id
     */
    private String datasetId;
    /**
     * 文档id
     */
    private String documentId;
    /**
     * 向量数据库里的对应segment id
     */
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
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 文档名称
     */
    private String documentName;
    /**
     * 相似度分数
     */
    private double score;
    /**
     * 文件id
     */
    private String fileId;
}
