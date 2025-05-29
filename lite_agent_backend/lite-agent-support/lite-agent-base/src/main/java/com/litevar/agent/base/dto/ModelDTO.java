package com.litevar.agent.base.dto;

import lombok.Data;

/**
 * @author uncle
 * @since 2024/10/12 15:11
 */
@Data
public class ModelDTO {
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
     * 创建人名称
     */
    private String createUser;

    /**
     * 限制最大token
     */
    private Integer maxTokens;
    /**
     * 模型类型: text, embedding, image, video...
     */
    private String type = "text";

    /**
     * 是否能编辑
     */
    private Boolean canEdit = Boolean.FALSE;

    /**
     * 是否能删除
     */
    private Boolean canDelete = Boolean.FALSE;

    /**
     * 是否能读
     */
    private Boolean canRead = Boolean.TRUE;
}
