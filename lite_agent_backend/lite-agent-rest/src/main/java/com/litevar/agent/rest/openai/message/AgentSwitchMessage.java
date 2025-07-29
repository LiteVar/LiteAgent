package com.litevar.agent.rest.openai.message;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * agent切换消息
 *
 * @author uncle
 * @since 2025/4/16 12:20
 */
@Data
@AllArgsConstructor
public class AgentSwitchMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    private String agentName;
}
