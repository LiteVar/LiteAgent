package com.litevar.agent.base.vo;

import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 输出前端的message
 *
 * @author uncle
 * @since 2024/9/2 11:58
 */
@Data
public class OutMessage {
    private String role;
    /**
     * 消息类型:text,imageUrl,functionCallList,toolReturn,flag
     */
    private String type;
    private Object content;

    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<FunctionCall> toolCalls;
    private String toolCallId;

    public OutMessage() {
        this.createTime = LocalDateTime.now();
    }

    @Data
    public static class FunctionCall {
        private String id;
        private String name;
        private JSONObject arguments;
    }
}
