package com.litevar.agent.core.module.tool.executor;

import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.opentool.model.StreamEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * open tool 协议调用
 * 通过第三方系统实现调用,并回调结果
 *
 * @author uncle
 * @since 2025/4/10 15:26
 */
@Slf4j
@Component
public class OpenToolThirdExecutor implements FunctionExecutor, InitializingBean {
    Map<String, String> resultMap = new ConcurrentHashMap<>();
    Map<String, CompletableFuture<String>> futureMap = new ConcurrentHashMap<>();
    Map<String, StreamState> streamStateMap = new ConcurrentHashMap<>();

    public void callback(String callId, String result) {
        resultMap.computeIfAbsent(callId, k -> result);
        if (futureMap.containsKey(callId)) {
            futureMap.get(callId).complete(result);
        }
    }

    public void streamCallback(String callId, StreamEventType eventType, String result) {
        StreamState state = streamStateMap.computeIfAbsent(callId, k -> new StreamState());
        if (eventType == StreamEventType.START) {
            state.onStart();
            return;
        }
        if (eventType == StreamEventType.DONE) {
            state.onDone();
            return;
        }
        state.onData(result);
    }

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        if (resultMap.containsKey(callId)) {
            return resultMap.remove(callId);
        }
        CompletableFuture<String> future = futureMap.computeIfAbsent(callId, k -> new CompletableFuture<>());
        String result;
        log.info("开始等待获取回调结果:callId:{}", callId);
        try {
            result = future.get(5, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            result = "call timeout";
            log.error("wait for callback timeout,callId:{}", callId);
        }
        resultMap.remove(callId);
        futureMap.remove(callId);
        return result;
    }

    @Override
    public void streamCall(String callId, ToolFunction info, Map<String, Object> data,
                           Consumer<List<String>> onStream) {
        StreamState state = streamStateMap.computeIfAbsent(callId, k -> new StreamState());
        CompletableFuture<String> future = new CompletableFuture<>();
        state.attach(onStream, future);
        log.info("开始等待获取流式回调结果:callId:{}", callId);
        try {
            future.get(10, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException e) {
            log.error("stream call failure,callId:{}", callId, e);
            state.onData("call error");
            state.onDone();
        } catch (TimeoutException e) {
            log.warn("stream call timeout,callId:{}", callId);
            state.onData("call timeout");
            state.onDone();
        } finally {
            streamStateMap.remove(callId);
        }
    }

    private static class StreamState {
        private static final String START_PLACEHOLDER = "";
        private final Queue<String> queue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean draining = new AtomicBoolean(false);
        private final AtomicBoolean startHandled = new AtomicBoolean(false);
        private final AtomicBoolean doneReceived = new AtomicBoolean(false);
        private volatile CompletableFuture<String> future;
        private volatile Consumer<List<String>> handler;

        void attach(Consumer<List<String>> handler, CompletableFuture<String> future) {
            this.future = future;
            this.handler = handler;
            drain();
            if (doneReceived.get()) {
                future.complete("");
            }
        }

        void onStart() {
            ensureStart();
        }

        void ensureStart() {
            if (startHandled.compareAndSet(false, true)) {
                queue.offer(START_PLACEHOLDER);
                drain();
            }
        }

        void onData(String data) {
            ensureStart();
            queue.offer(data);
            drain();
        }

        void onDone() {
            ensureStart();
            doneReceived.set(true);
            queue.offer(STREAM_DONE);
            drain();
            CompletableFuture<String> future = this.future;
            if (future != null) {
                future.complete("");
            }
        }

        private void drain() {
            Consumer<List<String>> handler = this.handler;
            if (handler == null) {
                return;
            }
            if (!draining.compareAndSet(false, true)) {
                return;
            }
            try {
                while (true) {
                    List<String> list = new ArrayList<>();
                    String single;
                    while ((single = queue.poll()) != null) {
                        list.add(single);
                    }
                    if (list.isEmpty()) {
                        return;
                    }
                    handler.accept(list);
                }
            } finally {
                draining.set(false);
                if (!queue.isEmpty()) {
                    drain();
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(EXTERNAL, this);
    }
}
