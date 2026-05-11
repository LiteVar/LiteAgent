package com.litevar.agent.rest.agentflow.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author uncle
 * @since 2025/12/17 10:52
 */
@Setter
@Getter
public class AgentContext {
    private String sessionId;
    private String agentId;
    private String parentTaskId;
    private String taskId;
    private String requestId;
    private String userId;
    private boolean stream;

    private AgentExecutionSpec runtimeInfo;
}
