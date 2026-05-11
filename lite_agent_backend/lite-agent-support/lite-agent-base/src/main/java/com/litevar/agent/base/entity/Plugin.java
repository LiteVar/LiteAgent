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
 * 插件
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Data
@CollectionName("plugin")
public class Plugin {
    /**
     * 插件ID
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 插件名称
     */
    @MongoIndex
    private String name;

    /**
     * 状态
     *
     * @see com.litevar.agent.base.enums.PluginStatus
     */
    private Integer status;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 运行地址
     */
    private String url;

    /**
     * 插件包文件名
     */
    private String packageName;

    /**
     * 插件包下载地址
     */
    private String packageUrl;

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
