package com.litevar.agent.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * agent聊天用户发送的消息
 *
 * @author uncle
 * @since 2024/9/13 12:17
 */
@Data
public class AgentSendMsgDTO {
    /**
     * @see MessageType
     */
    @NotBlank
    private String type;
    /**
     * type=imageUrl时,内容为url或fileId(通过上传chat文件接口得到)
     * type=videoUrl时,内容为url或fileId(通过上传chat文件接口得到)
     * type=execute时,内容为planId
     */
    @NotBlank
    private String message;

    @Getter
    @AllArgsConstructor
    public enum MessageType {
        TEXT("text"),
        IMAGE_URL("imageUrl"),
        VIDEO_URL("videoUrl"),
        EXECUTE("execute");

        public final String type;

        public static MessageType of(String type) {
            for (MessageType value : values()) {
                if (value.type.equals(type)) {
                    return value;
                }
            }
            return null;
        }
    }
}
