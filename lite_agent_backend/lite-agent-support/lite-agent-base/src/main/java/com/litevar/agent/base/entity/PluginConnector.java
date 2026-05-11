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
 * 插件connector
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Data
@CollectionName("plugin_connector")
public class PluginConnector {
    /**
     * connector id
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 插件ID
     */
    @MongoIndex
    private String pluginId;

    /**
     * 工作空间ID
     */
    @MongoIndex
    private String workspaceId;

    /**
     * connector 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 状态
     *
     * @see com.litevar.agent.base.enums.PluginStatus
     */
    private Integer status;

    /**
     * 创建人ID
     */
    private String userId;

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

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";
}
