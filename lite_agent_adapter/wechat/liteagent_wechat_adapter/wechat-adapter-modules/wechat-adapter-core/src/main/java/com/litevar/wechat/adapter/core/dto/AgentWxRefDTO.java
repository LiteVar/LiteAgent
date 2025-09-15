package com.litevar.wechat.adapter.core.dto;

import com.mongoplus.annotation.ID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *  agent与服务号绑定对象
 * @author Teoan
 * @since 2025/8/13 17:11
 */
@Data
@Schema(description = "agent与服务号绑定对象DTO")
public class AgentWxRefDTO {

    /**
     * 唯一标识
     */
    @ID
    @Schema(description = "唯一标识")
    private String id;


    /**
     * 关系名称
     */
    @Schema(description = "关系名称")
    @NotBlank(message = "关系名称不能为空")
    private String name;


    /**
     * AgentApiKey
     */
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
     * 服务号的appId
     */
    @Schema(description = "服务号的appId")
    @NotBlank(message = "appId不能为空")
    private String appId;

    /**
     * 服务号的appSecret
     */
    @Schema(description = "服务号的appSecret")
    @NotBlank(message = "appSecret不能为空")
    private String appSecret;


    /**
     * 服务号的 token
     */
    @Schema(description = "服务号的 token")
    @NotBlank(message = "token不能为空")
    private String token;


    /**
     * 服务号的 消息加密密钥
     */
    @Schema(description = "服务号的消息加密密钥")
    private String aesKey;

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
