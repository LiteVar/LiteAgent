package com.litevar.agent.rest.openai.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常消息
 *
 * @author uncle
 * @since 2025/4/15 17:48
 */
@Getter
@AllArgsConstructor
public class ErrorMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    private Throwable ex;
}
