package com.litevar.agent.rest.openai.message;

import cn.hutool.core.util.IdUtil;
import com.litevar.agent.rest.util.CurrentAgentRequest;
import lombok.Getter;

/**
 * agent分发消息
 *
 * @author uncle
 * @since 2025/4/11 17:05
 */
@Getter
public class DistributeMessage implements AgentMessage {
    private final String sessionId;
    private final String taskId;
    private final String agentId;
    private final String requestId;
    private final String parentTaskId;

    private final String cmd;
    private final String targetAgentId;
    private final String dispatchId;

    public DistributeMessage(String cmd, String targetAgentId,String taskId) {
        this.sessionId = CurrentAgentRequest.getSessionId();
        this.taskId = taskId;
        this.agentId = targetAgentId;
        this.requestId = CurrentAgentRequest.getRequestId();
        this.parentTaskId = CurrentAgentRequest.getTaskId();

        this.cmd = cmd;
        this.targetAgentId = targetAgentId;
        this.dispatchId = IdUtil.getSnowflakeNextIdStr();
    }
}
