package com.litevar.agent.rest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 导入进度发布器
 */
@Slf4j
@Service
public class AgentImportProgressPublisher {

    private static final int DEFAULT_REPLAY_LIMIT = 64;
    private final Map<String, Sinks.Many<String>> sinkRegistry = new ConcurrentHashMap<>();

    public Flux<String> listen(String token) {
        return Flux.defer(() -> sinkRegistry
                .computeIfAbsent(token, key -> Sinks.many().replay().limit(DEFAULT_REPLAY_LIMIT))
                .asFlux());
    }

    public void publishError(String token, String message) {
        publish(token, message);
        complete(token);
    }

    public void publish(String token, String message) {
        if (token == null || message == null) {
            return;
        }
        Sinks.Many<String> sink = sinkRegistry.get(token);
        if (sink == null) {
            return;
        }
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
                log.warn("Agent import progress overflow for token={}", token);
            } else if (result == Sinks.EmitResult.FAIL_CANCELLED) {
                log.debug("Agent import progress sink cancelled for token={}", token);
            } else {
                log.debug("Agent import progress emit failed for token={}, reason={}", token, result);
            }
        }
    }

    public void complete(String token) {
        if (token == null) {
            return;
        }
        Sinks.Many<String> sink = sinkRegistry.remove(token);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
