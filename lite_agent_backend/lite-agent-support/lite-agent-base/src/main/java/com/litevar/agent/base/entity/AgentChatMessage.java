package com.litevar.agent.base.entity;

import com.litevar.agent.base.vo.OutMessage;
import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * agent聊天消息
 *
 * @author uncle
 * @since 2024/9/2 18:04
 */
@Data
@CollectionName("agent_chat_message")
public class AgentChatMessage {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String userId;

    private String agentId;

    @MongoIndex
    private String sessionId;

    private String model;

    @CollectionField
    private List<TaskMessage> taskMessage;

    /**
     * @see com.litevar.agent.base.enums.AgentCallType
     */
    private Integer callType = 0;

    /**
     * 0-正常聊天,1-调试
     */
    private Integer debugFlag = 0;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";

    @Data
    public static class TaskMessage {
        private String taskId;
        private List<OutMessage> message;
    }
}
