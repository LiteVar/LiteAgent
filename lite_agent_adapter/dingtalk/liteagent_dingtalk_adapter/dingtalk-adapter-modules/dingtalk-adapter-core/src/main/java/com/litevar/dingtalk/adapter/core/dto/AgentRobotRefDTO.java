package com.litevar.dingtalk.adapter.core.dto;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.index.MongoIndex;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *  agent与机器人绑定对象
 * @author Teoan
 * @since 2025/8/13 17:11
 */
@Data
@Schema(description = "agent与机器人绑定对象DTO")
public class AgentRobotRefDTO  {

    /**
     * 唯一标识
     */
    @ID
    @Schema(description = "唯一标识")
    private String id;


    /**
     * 关系名称
     */
    @MongoIndex
    @Schema(description = "关系名称")
    @NotBlank(message = "关系名称不能为空")
    private String name;


    /**
     * AgentApiKey
     */
    @MongoIndex
    @Schema(description = "AgentApiKey")
    @NotBlank(message = "AgentApiKey不能为空")
    private String agentApiKey;


    /**
     * AgentBaseUrl
     */
    @Schema(description = "AgentBaseUrl")
    @NotBlank(message = "AgentBaseUrl不能为空")
    private String agentBaseUrl;


    /**
     * 机器人code
     */
    @MongoIndex
    @Schema(description = "机器人code")
    @NotBlank(message = "机器人code不能为空")
    private String robotCode;


    /**
     * 机器人应用的clientId
     */
    @Schema(description = "机器人应用的clientId")
    @NotBlank(message = "机器人应用的clientId不能为空")
    private String robotClientId;


    /**
     * 机器人应用的clientSecret
     */
    @Schema(description = "机器人应用的clientSecret")
    @NotBlank(message = "机器人应用的clientSecret不能为空")
    private String robotClientSecret;


    /**
     * 卡片模板Id
     */
    @Schema(description = "卡片模板Id")
    @NotBlank(message = "卡片模板Id不能为空")
    private String cardTemplateId;

    /**
     * 权限信息
     */
    @Schema(description = "权限信息")
    private RobotPermissionsDTO robotPermissionsDTO;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
