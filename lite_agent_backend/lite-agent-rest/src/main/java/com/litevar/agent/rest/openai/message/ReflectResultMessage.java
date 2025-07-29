package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.response.ReflectResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 反思消息
 *
 * @author uncle
 * @since 2025/4/11 16:55
 */
@Data
@AllArgsConstructor
public class ReflectResultMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;
    private String agentName;

    private String rawInput;
    private String rawOutput;
    private List<ReflectResult> reflectOutput;
}
