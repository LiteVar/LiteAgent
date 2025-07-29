package com.litevar.agent.rest.openai.message;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 工具调用结果消息
 *
 * @author uncle
 * @since 2025/4/11 16:50
 */
@Data
@AllArgsConstructor
public class ToolResultMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    private String callId;
    private String result;
    private String functionName;
}
