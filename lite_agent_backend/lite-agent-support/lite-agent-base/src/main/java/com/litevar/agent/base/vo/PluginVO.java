package com.litevar.agent.base.vo;

import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * Plugin form.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Data
public class PluginVO {
    /**
     * 插件ID
     */
    @Null(groups = AddAction.class)
    @NotBlank(groups = UpdateAction.class)
    private String id;

    /**
     * 插件名称
     */
    @NotBlank
    private String name;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;
}
