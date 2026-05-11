package com.litevar.agent.rest.agentflow;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Request-level stop state for agent execution.
 *
 * @author uncle
 * @since 2025/12/23 17:38
 */
@Component
public class ExecutionStopManager {
    private final Map<String, StopState> stateMap = new ConcurrentHashMap<>();

    public void init(String requestId) {
        stateMap.put(requestId, new StopState());
    }

    public void clear(String requestId) {
        stateMap.remove(requestId);
    }

    /**
     * 停止agent流程
     */
    public void requestStop(String requestId) {
        StopState state = stateMap.get(requestId);
        if (state == null) {
            return;
        }
        state.stopRequested.set(true);
        if (state.toolRunning.get() == 0) {
            cancelLlm(state);
        }
    }

    public boolean shouldStop(String requestId) {
        StopState state = stateMap.get(requestId);
        return state != null && state.stopRequested.get() && state.toolRunning.get() == 0;
    }

    public boolean isStopRequested(String requestId) {
        StopState state = stateMap.get(requestId);
        return state != null && state.stopRequested.get();
    }

    public void markToolStart(String requestId) {
        StopState state = stateMap.get(requestId);
        if (state != null) {
            state.toolRunning.incrementAndGet();
        }
    }

    public void markToolEnd(String requestId) {
        StopState state = stateMap.get(requestId);
        if (state == null) {
            return;
        }
        int running = state.toolRunning.decrementAndGet();
        if (running < 0) {
            state.toolRunning.set(0);
        }
        if (state.stopRequested.get() && state.toolRunning.get() == 0) {
            cancelLlm(state);
        }
    }

    public void registerLlmHandle(String requestId, Runnable cancel) {
        StopState state = stateMap.get(requestId);
        if (state == null) {
            return;
        }
        state.llmCancel.set(cancel);
        if (state.stopRequested.get() && state.toolRunning.get() == 0) {
            cancelLlm(state);
        }
    }

    public void clearLlmHandle(String requestId, Runnable cancel) {
        StopState state = stateMap.get(requestId);
        if (state != null) {
            state.llmCancel.compareAndSet(cancel, null);
        }
    }

    private void cancelLlm(StopState state) {
        Runnable cancel = state.llmCancel.get();
        if (cancel != null) {
            cancel.run();
        }
    }

    private static class StopState {
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);
        private final AtomicInteger toolRunning = new AtomicInteger(0);
        private final AtomicReference<Runnable> llmCancel = new AtomicReference<>();

        private StopState() {

        }
    }
}
