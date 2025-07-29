package com.litevar.agent.rest.openai.message;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author uncle
 * @since 2025/5/7 16:36
 */
@Data
@AllArgsConstructor
public class OpenToolMessage implements AgentMessage {
    private String agentId;
    private String sessionId;
    private String taskId;

    private String callId;
    private String name;
    private JSONObject arguments;
}
