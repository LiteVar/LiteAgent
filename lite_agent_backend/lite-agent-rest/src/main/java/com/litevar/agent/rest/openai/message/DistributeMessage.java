package com.litevar.agent.rest.openai.message;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * agent分发消息
 *
 * @author uncle
 * @since 2025/4/11 17:05
 */
@Data
@AllArgsConstructor
public class DistributeMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    private String cmd;
    private String targetAgentId;
    private String dispatchId;
}
