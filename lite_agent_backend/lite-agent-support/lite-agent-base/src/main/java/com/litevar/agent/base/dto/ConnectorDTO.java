package com.litevar.agent.base.dto;

import lombok.Data;

/**
 * @author uncle
 * @since 2026/1/13 11:32
 */
@Data
public class ConnectorDTO {
    /**
     * connector id
     */
    private String id;

    /**
     * 插件ID
     */
    private String pluginId;

    /**
     * 插件名称
     */
    public String pluginName;

    /**
     * 插件描述
     */
    public String pluginDescription;

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
     * 插件状态
     *
     * @see com.litevar.agent.base.enums.PluginStatus
     */
    private Integer pluginStatus;

    /**
     * 是否能编辑
     */
    private boolean canEdit = false;

    /**
     * 创建人名称
     */
    private String createUser;
}
