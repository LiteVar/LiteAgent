package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息清除流水
 *
 * @author uncle
 * @since 2025/6/16 12:13
 */
@Data
@CollectionName("agent_chat_message_clear")
public class AgentChatMessageClear {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String userId;

    private String agentId;

    private Integer debugFlag;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
