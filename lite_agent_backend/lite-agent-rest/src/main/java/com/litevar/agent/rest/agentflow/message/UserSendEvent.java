package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

import java.util.List;

/**
 * 用户发送消息
 *
 * @author uncle
 * @since 2025/12/18 15:33
 */
public record UserSendEvent(List<AgentSendMsgDTO> messageList) implements AgentEventPayload {

    @Override
    public AgentEventType type() {
        return AgentEventType.USER_SEND_EVENT;
    }
}
