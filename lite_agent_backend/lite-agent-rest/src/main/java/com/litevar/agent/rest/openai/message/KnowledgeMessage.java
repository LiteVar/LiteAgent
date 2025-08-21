package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

import java.util.List;

/**
 * 知识库消息
 *
 * @author uncle
 * @since 2025/4/17 18:33
 */
@Getter
public class KnowledgeMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String agentId;
    private final String requestId;
    private final String parentTaskId;

    /**
     * 检索的内容
     */
    private final String retrieveContent;

    /**
     * 检索历史信息
     */
    private final List<OutMessage.KnowledgeHistoryInfo> historyInfo;

    public KnowledgeMessage(String retrieveContent, List<OutMessage.KnowledgeHistoryInfo> historyInfo) {
        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = CurrentAgentRequest.getTaskId();
        this.agentId = CurrentAgentRequest.getAgentId();
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getContext().getParentTaskId();

        this.retrieveContent = retrieveContent;
        this.historyInfo = historyInfo;
    }
}
