package com.litevar.agent.base.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 外部API 发送消息
 *
 * @author uncle
 * @since 2025/4/9 09:55
 */
@Data
public class ExternalSendMsgDTO {
    private Boolean isChunk = true;
    @NotNull
    private List<Content> content;

    @Data
    public static class Content {
        /**
         * text,imageUrl,videoUrl,execute
         */
        private String type;
        /**
         * type=imageUrl时,内容为url或fileId(通过上传chat文件接口得到)
         * type=videoUrl时,内容为url或fileId(通过上传chat文件接口得到)
         * type=execute时,内容为planId
         */
        private String message;
    }
}
