package com.litevar.agent.base.dto;

import lombok.Data;

import java.util.List;

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
     * 创建人名称
     */
    private String createUser;

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
     * 是否已发布
     */
    private Integer status;

    /**
     * agent类型
     *
     * @see com.litevar.agent.base.enums.AgentType
     */
    private Integer type;
    /**
     * 执行模式
     *
     * @see com.litevar.agent.base.enums.ExecuteMode
     */
    private Integer mode = 0;
    /**
     * 知识库id
     */
    private List<String> datasetIds;
}
