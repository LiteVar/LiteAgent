package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author reid
 * @since 3/13/25
 */

@Data
@CollectionName("dataset_retrieve_history")
public class DatasetRetrieveHistory {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String datasetId;
    private String agentId;
    /**
     * 检索内容
     */
    private String content;
    /**
     * 召回类型: TEST,AGENT
     */
    private String retrieveType;

    /**
     * 检索到的片段信息
     */
    private List<RetrieveSegment> retrieveSegmentList;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Data
    public static class RetrieveSegment {
        /**
         * 片段id
         */
        private String id;
        /**
         * 知识库id
         */
        private String datasetId;
        /**
         * 文档id
         */
        private String documentId;
        /**
         * token数
         */
        private Integer tokenCount = 0;
        /**
         * 相似度分数
         */
        private double score;
    }
}
