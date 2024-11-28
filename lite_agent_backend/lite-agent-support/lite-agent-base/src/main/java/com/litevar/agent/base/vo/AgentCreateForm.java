package com.litevar.agent.base.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author reid
 * @since 2024/8/12
 */
@Data
public class AgentCreateForm {
    /**
     * agent名称
     */
    @NotBlank
    private String name;
    /**
     * 图标url
     */
    private String icon = "";
    /**
     * 描述,可空
     */
    private String description = "";
}
