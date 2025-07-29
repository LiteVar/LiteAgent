package com.litevar.agent.base.dto;

import com.litevar.agent.base.entity.AgentChatMessageClear;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author uncle
 * @since 2025/6/16 14:27
 */
@Data
public class MessageAndClearDTO {
    private List<MessageDTO> messageList;
    private List<AgentChatMessageClear> clearList = Collections.emptyList();
}
