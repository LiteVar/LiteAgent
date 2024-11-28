package com.litevar.agent.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * agent聊天用户发送的消息
 *
 * @author uncle
 * @since 2024/9/13 12:17
 */
@Data
public class AgentSendMsgDTO {
    /**
     * text,imageUrl
     */
    @NotBlank
    private String type;
    /**
     * type=imageUrl时,格式为: data:image/jpeg;base64,{图片的base64编码}
     */
    @NotBlank
    private String message;

}