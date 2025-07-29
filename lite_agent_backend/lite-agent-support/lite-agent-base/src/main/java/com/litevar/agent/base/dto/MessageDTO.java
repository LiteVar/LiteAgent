package com.litevar.agent.base.dto;

import com.litevar.agent.base.entity.AgentChatMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author uncle
 * @since 2025/3/20 12:13
 */
@Data
public class MessageDTO {
    private String sessionId;
    private List<AgentChatMessage.TaskMessage> taskMessage;
    /**
     * 来源: api,user,debug
     */
    private String origin;

    private LocalDateTime createTime;

    private String user;
}
