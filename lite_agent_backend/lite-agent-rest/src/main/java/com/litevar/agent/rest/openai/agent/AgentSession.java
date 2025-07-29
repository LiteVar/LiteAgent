package com.litevar.agent.rest.openai.agent;

import com.litevar.agent.rest.openai.handler.AgentMessageHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author uncle
 * @since 2025/3/5 15:00
 */
@Getter
public class AgentSession {
    @Setter
    private MultiAgent agent;
    private final List<AgentMessageHandler> handlers = new CopyOnWriteArrayList<>();
}
