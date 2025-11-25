package com.litevar.agent.base.vo;

import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * @author uncle
 * @since 2024/8/13 17:04
 */
@Data
public class ToolVO {
    /**
     * 工具id
     */
    @Null(groups = AddAction.class)
    @NotBlank(groups = UpdateAction.class)
    private String id;
    /**
     * 工具名称
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
     * schema类型
     *
     * @see ToolSchemaType#getValue()
     */
    private Integer schemaType;
    /**
     * schema文稿
     */
    private String schemaStr;

    /**
     * api key类型:Bearer、Basic
     */
    private String apiKeyType;
    /**
     * api key 值
     */
    private String apiKey;

    /**
     * 是否支持auto agent使用
     */
    private Boolean autoAgent = Boolean.FALSE;

    //=========== 以下字段为导入时用到

    /**
     * 相同的数据id
     */
    private String similarId;

    /**
     * 数据操作类型
     *
     * @see com.litevar.agent.base.enums.OperateTypeEnum
     */
    private Integer operate = 0;
}
