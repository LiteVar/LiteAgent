package com.litevar.agent.base.dto;

import lombok.Data;

/**
 * 列表返回
 *
 * @author uncle
 * @since 2024/10/12 17:08
 */
@Data
public class AgentDTO {
    private String id;

    /**
     * 创建者id
     */
    private String userId;

    /**
     * agent名称
     */
    private String name;
    /**
     * 图标url
     */
    private String icon = "";
    /**
     * 描述,可空
     */
    private String description = "";

    /**
     * 是否已分享
     */
    private Boolean shareTip;
    /**
     * 是否已发布
     */
    private Integer status;
}
