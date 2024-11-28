package com.litevar.agent.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author uncle
 * @since 2024/9/3 16:20
 */
@Getter
public class AgentMessageEvent extends ApplicationEvent {
    private String sessionId;
    private String taskId;

    public AgentMessageEvent(Object source, String sessionId, String taskId) {
        super(source);
        this.sessionId = sessionId;
        this.taskId = taskId;
    }
}
