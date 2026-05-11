package com.litevar.agent.rest.agentflow.message;

import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.rest.agentflow.event.AgentEventPayload;
import com.litevar.agent.rest.agentflow.event.AgentEventType;

import java.util.List;

/**
 * 知识库调用消息
 *
 * @author uncle
 * @since 2025/12/18 15:52
 */
public record KnowledgeEvent(String retrieveContent,
                           List<OutMessage.KnowledgeHistoryInfo> historyInfo) implements AgentEventPayload {
    @Override
    public AgentEventType type() {
        return AgentEventType.KNOWLEDGE_EVENT;
    }
}
