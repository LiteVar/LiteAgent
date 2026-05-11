package com.litevar.agent.base.vo;

import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * Plugin connector form.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Data
public class PluginConnectorVO {
    /**
     * 智连ID
     */
    @Null(groups = AddAction.class)
    @NotBlank(groups = UpdateAction.class)
    private String id;

    /**
     * 插件ID
     */
    @NotBlank(groups = AddAction.class)
    private String pluginId;

    /**
     * 智连名称
     */
    @NotBlank
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
     * 根据插件schema结构填写的数据
     */
    @NotNull
    private Object data;
}
