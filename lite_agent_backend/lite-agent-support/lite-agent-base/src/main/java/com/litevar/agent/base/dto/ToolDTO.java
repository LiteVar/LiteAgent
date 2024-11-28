package com.litevar.agent.base.dto;

import lombok.Data;

/**
 * 工具管理列表返回数据
 *
 * @author uncle
 * @since 2024/8/14 11:43
 */
@Data
public class ToolDTO {
    /**
     * 工具id
     */
    private String id;
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
     * 是否已分享
     */
    private Boolean shareTip;

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
