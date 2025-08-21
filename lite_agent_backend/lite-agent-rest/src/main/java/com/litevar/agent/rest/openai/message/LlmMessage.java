package com.litevar.agent.rest.openai.message;

import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * 大模型返回的文本消息
 *
 * @author uncle
 * @since 2025/4/11 12:29
 */
@Getter
public class LlmMessage implements AgentMessage {
    private final String sessionId;
    private final String requestId;
    private final String parentTaskId;
    private final String taskId;
    private final String agentId;
    private final Integer agentType;

    private final CompletionResponse response;

    public LlmMessage(Integer agentType, CompletionResponse response) {
        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = CurrentAgentRequest.getTaskId();
        this.agentId = CurrentAgentRequest.getAgentId();
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getContext().getParentTaskId();

        this.agentType = agentType;
        this.response = response;
    }
}
