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
 * OpenApi provider entity
 *
 * @author reid
 * @since 2024/7/29
 */

@Data
@CollectionName("tool_provider")
public class ToolProvider {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 创建人
     */
    private String userId;
    /**
     * 工作空间id
     */
    @MongoIndex
    private String workspaceId;

    /**
     * 工具名称
     */
    private String name;
    private String icon;
    /**
     * 描述
     */
    private String description;
    /**
     * schema类型
     *
     * @see com.litevar.agent.base.enums.ToolSchemaType#value
     */
    private Integer schemaType;
    /**
     * 原始schema描述字符串, yml、json
     */
    private String schemaStr;

    /**
     * open tool schema
     */
    @Deprecated
    private String openSchemaStr;

    /**
     * 调用工具的apiKey
     */
    private String apiKey;
    /**
     * apiKey类型: Bearer、Basic
     */
    private String apiKeyType;

    /**
     * 是否支持auto agent使用
     */
    private Boolean autoAgent = Boolean.FALSE;

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
