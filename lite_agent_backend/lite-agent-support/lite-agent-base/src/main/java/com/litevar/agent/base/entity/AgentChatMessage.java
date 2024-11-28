package com.litevar.agent.base.entity;

import com.litevar.agent.base.vo.OutMessage;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * agent聊天消息
 *
 * @author uncle
 * @since 2024/9/2 18:04
 */
@Data
@Document("agent_chat_message")
public class AgentChatMessage {
    private String id;

    private String userId;

    private String agentId;

    private String sessionId;

    private String model;

    private List<TaskMessage> taskMessage;

    /**
     * 0-正常聊天,1-调试
     */
    private Integer debugFlag = 0;

    @CreatedDate
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    private Boolean deleted = Boolean.FALSE;

    @Data
    public static class TaskMessage {
        private String taskId;
        private List<OutMessage> message;
        private TokenUsage tokenUsage;
    }

    @Data
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
