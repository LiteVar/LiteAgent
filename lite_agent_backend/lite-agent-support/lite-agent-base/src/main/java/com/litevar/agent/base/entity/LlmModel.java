package com.litevar.agent.base.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 大语言模型表
 *
 * @author reid
 * @since 2024/8/1
 */

@Data
@Document(collection = "llm_model")
public class LlmModel {
    @Id
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
    @Indexed
    private String workspaceId;
    /**
     * 是否共享
     */
    private Boolean shareFlag;

    /**
     * 限制最大token
     */
    private Integer maxTokens;

    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

}
