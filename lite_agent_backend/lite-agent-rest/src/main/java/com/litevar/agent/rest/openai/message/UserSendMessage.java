package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.dto.AgentSendMsgDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 用户发送的消息
 *
 * @author uncle
 * @since 2025/4/11 12:19
 */
@Data
@AllArgsConstructor
public class UserSendMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    private List<AgentSendMsgDTO> messageList;
}
