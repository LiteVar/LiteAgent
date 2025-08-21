package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.response.ReflectResult;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

import java.util.List;

/**
 * 反思消息
 *
 * @author uncle
 * @since 2025/4/11 16:55
 */
@Getter
public class ReflectResultMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String agentId;
    private final String agentName;
    private final String requestId;
    private final String parentTaskId;

    private final String rawInput;
    private final String rawOutput;
    private final List<ReflectResult> reflectOutput;

    public ReflectResultMessage(String agentName, String rawInput, String rawOutput, List<ReflectResult> reflectOutput) {
        this.rawInput = rawInput;
        this.rawOutput = rawOutput;
        this.reflectOutput = reflectOutput;
        this.agentName = agentName;

        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = CurrentAgentRequest.getTaskId();
        this.agentId = CurrentAgentRequest.getAgentId();
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getContext().getParentTaskId();
    }
}
