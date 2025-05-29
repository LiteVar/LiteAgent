package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大语言模型表
 *
 * @author reid
 * @since 2024/8/1
 */
@Data
@CollectionName("llm_model")
public class LlmModel {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 模型名称
     */
    private String name;
    /**
     * 模型访问url
     */
    private String baseUrl;
    /**
     * 模型访问key
     */
    private String apiKey;
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
     * 模型类型: text, embedding, asr, tts, image...
     */
    private String type = "LLM";

    /**
     * 限制最大token
     */
    private Integer maxTokens;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @CollectionField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";
}
