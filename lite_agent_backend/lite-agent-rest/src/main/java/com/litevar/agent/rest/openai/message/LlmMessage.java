package com.litevar.agent.rest.openai.message;

import com.litevar.agent.openai.completion.CompletionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 大模型返回的文本消息
 *
 * @author uncle
 * @since 2025/4/11 12:29
 */
@Data
@AllArgsConstructor
public class LlmMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;
    private Integer agentType;

    private CompletionResponse response;
}
