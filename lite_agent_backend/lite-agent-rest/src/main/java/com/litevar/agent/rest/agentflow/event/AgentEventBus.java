package com.litevar.agent.rest.agentflow.event;

import com.litevar.agent.rest.agentflow.listener.LogAgentEventListener;
import com.litevar.agent.rest.agentflow.listener.StoreMsgEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author uncle
 * @since 2025/12/18 16:41
 */
@Slf4j
@Component
public class AgentEventBus {
    private final Map<String, AgentEventListener> listenerMap = new ConcurrentHashMap<>();

    private final LogAgentEventListener logListener = new LogAgentEventListener();
    private final StoreMsgEventListener storeListener = new StoreMsgEventListener();

    public void register(String requestId, AgentEventListener listener) {
        listenerMap.put(requestId, listener);
    }

    public void unregister(String requestId) {
        listenerMap.remove(requestId);
    }

    public void publish(AgentEvent event) {
        AgentEventListener listener = listenerMap.get(event.getRequestId());
        if (listener != null) {
            listener.onEvent(event);
        }
        logListener.onEvent(event);
        storeListener.onEvent(event);
    }
}
