package com.litevar.agent.rest.agentflow.agent;

import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.openai.OpenAiChatModel;
import com.litevar.agent.openai.RequestExecutor;
import com.litevar.agent.openai.completion.CompletionCallback;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.rest.agentflow.ExecutionStopManager;
import com.litevar.agent.rest.agentflow.bean.AgentContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * @author uncle
 * @since 2025/12/17 11:29
 */
@Component
public class AgentEngine {

    @Resource
    private ExecutionStopManager stopManager;

    public CompletionResponse decide(AgentContext context, List<Message> messages, BiConsumer<String, Integer> onChunk) {
        return invokeModel(context, messages, onChunk);
    }

    private CompletionResponse invokeModel(AgentContext context, List<Message> messages, BiConsumer<String, Integer> onChunk) {
        if (stopManager.shouldStop(context.getRequestId())) {
            return null;
        }
        CompletableFuture<CompletionResponse> future = new CompletableFuture<>();
        CompletionCallback callback = new CompletionCallback() {
            @Override
            public void onPartialResponse(String taskId, String part, Integer chunkType) {
                if (onChunk != null) {
                    onChunk.accept(part, chunkType);
                }
            }

            @Override
            public void onError(String taskId, Throwable error) {
                if (stopManager.shouldStop(context.getRequestId())) {
                    future.complete(null);
                    return;
                }
                future.completeExceptionally(error);
            }

            @Override
            public void onCompleteResponse(String taskId, CompletionResponse response) {
                future.complete(response);
            }

            @Override
            public void start(String taskId) {

            }
        };
        TokenReportDTO report = new TokenReportDTO(context.getUserId(),
                context.getRuntimeInfo().getRequest().getLlmModelId(),
                context.getAgentId(), context.getSessionId());
        RequestExecutor.CallHandle<?> handle;
        if (context.isStream()) {
            handle = OpenAiChatModel.generate(context.getRuntimeInfo().getRequest(),
                    context.getTaskId(), messages, callback, report);
            handle.future().whenComplete((response, error) -> {
                if (error != null) {
                    future.complete(null);
                }
            });
        } else {
            callback.start(context.getTaskId());
            handle = OpenAiChatModel.generate(context.getRuntimeInfo().getRequest(),
                    context.getTaskId(), messages, report);
            handle.future().whenComplete((response, error) -> {
                if (error != null) {
                    callback.onError(context.getTaskId(), error);
                    return;
                }
                callback.onCompleteResponse(context.getTaskId(), (CompletionResponse) response);
            });
        }
        Runnable cancel = handle::cancel;
        stopManager.registerLlmHandle(context.getRequestId(), cancel);
        try {
            return future.join();
        } finally {
            stopManager.clearLlmHandle(context.getRequestId(), cancel);
        }
    }
}
