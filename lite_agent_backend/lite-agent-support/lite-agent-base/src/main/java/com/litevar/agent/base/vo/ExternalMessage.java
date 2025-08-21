package com.litevar.agent.base.vo;

import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONObject;
import com.litevar.agent.base.response.ReflectResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 封装给外部api的消息
 *
 * @author uncle
 * @since 2025/3/31 10:45
 */
@Data
public class ExternalMessage {
    /**
     * 会话id
     */
    private String sessionId;
    private String agentId;
    private String parentTaskId;
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 角色
     */
    private String role;
    /**
     * 消息发送的目标
     */
    private String to;
    /**
     * 消息类型(text,imageUrl,contentList,toolCalls,dispatch,reflection,toolReturn,functionCall)
     */
    private String type;
    /**
     * 消息内容
     */
    private Object content;

    /**
     * 大模型完成的详细信息
     */
    private Completions completions;

    private String createTime;

    private String part;

    public ExternalMessage() {
        this.createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    @Data
    public static class Completions {
        /**
         * token使用信息
         */
        private Usage usage;
        /**
         * 大模型返回的消息id
         */
        private String id;
        /**
         * 大模型名称
         */
        private String model;
    }

    @Data
    public static class Usage {
        /**
         * 提示词的token数
         */
        private Integer promptTokens;
        /**
         * 生成的token数
         */
        private Integer completionTokens;
        /**
         * 总的token数
         */
        private Integer totalTokens;
    }

    @Data
    public static class FunctionCall {
        private String id;
        private String name;
        private JSONObject arguments;
        private String toolId;
        private String toolName;
        private String functionId;
        private String functionName;
    }

    @Data
    public static class ToolReturn {
        private String id;
        private String result;
        private String functionName;
        private String toolId;
        private String toolName;
        private String functionId;
    }

    @Data
    public static class ReflectContent {
        private Boolean isPass;
        private String agentId;
        private String name;
        private MessageScore messageScore;
        private Integer passScore;
        private Integer count;
        private Integer maxCount = 10;
    }

    @Data
    public static class MessageScore {
        private String messageType;
        private String message;
        //[{type:text, message:xxx}]
        private List<Dict> content;
        private List<ReflectResult> reflectScoreList;
    }

    @Data
    public static class DistributeContent {
        private String dispatchId;
        private String agentId;
        private String name;
        //{type:xxx,message:xxx}
        private List<Dict> content;
    }
}
