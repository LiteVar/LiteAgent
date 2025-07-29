package com.litevar.agent.openai.completion;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.openai.completion.message.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * stream流的token拼接成完整的CompletionResponse,包括toolCalls
 *
 * @author uncle
 * @since 2025/2/19 15:30
 */
public class CompletionStreamResponseBuilder {
    private final StringBuffer contentBuilder = new StringBuffer();
    private final StringBuffer reasonBuilder = new StringBuffer();
    private final StringBuffer refusalBuilder = new StringBuffer();
    private final AtomicReference<String> id = new AtomicReference<>();
    private final AtomicReference<String> model = new AtomicReference<>();
    private final AtomicReference<CompletionResponse.Usage> tokenUsage = new AtomicReference<>();
    private final AtomicReference<String> finishReason = new AtomicReference<>();
    private final Map<Integer, ToolExecutionRequestBuilder> toolExecutionBuilder = new ConcurrentHashMap<>();

    public void append(CompletionResponse partial, AtomicInteger thinkFlag) {
        if (partial == null) {
            return;
        }
        if (StrUtil.isNotEmpty(partial.getId())) {
            this.id.set(partial.getId());
        }
        if (StrUtil.isNotEmpty(partial.getModel())) {
            this.model.set(partial.getModel());
        }
        if (partial.getUsage() != null) {
            this.tokenUsage.set(partial.getUsage());
        }
        List<CompletionResponse.Choices> choices = partial.getChoices();
        if (ObjectUtil.isEmpty(choices)) {
            return;
        }
        CompletionResponse.Choices choice = choices.get(0);
        if (choice == null) {
            return;
        }
        String finishReason = choice.getFinishReason();
        if (StrUtil.isNotEmpty(finishReason)) {
            this.finishReason.set(finishReason);
        }
        AssistantMessage delta = choice.getMessage();
        if (delta == null) {
            return;
        }
        String content = delta.getContent();
        String reasoningContent = delta.getReasoningContent();
        if (StrUtil.isNotEmpty(content)) {
            if (StrUtil.equals(content, "<think>")) {
                thinkFlag.set(1);
                delta.setContent("");
            } else if (StrUtil.equals(content, "</think>")) {
                thinkFlag.set(0);
                delta.setContent("");
            } else {
                if (thinkFlag.get() == 1) {
                    //如果是content中有<think></think>的情况,要把内容放到reasoningContent中
                    delta.setReasoningContent(content);
                    delta.setContent("");
                    this.reasonBuilder.append(content);
                } else {
                    this.contentBuilder.append(content);
                }
            }
        } else if (StrUtil.isNotEmpty(reasoningContent)) {
            this.reasonBuilder.append(reasoningContent);
        }
        String refusal = delta.getRefusal();
        if (StrUtil.isNotEmpty(refusal)) {
            this.refusalBuilder.append(refusal);
        }

        if (ObjectUtil.isNotEmpty(delta.getToolCalls())) {
            AssistantMessage.ToolCall toolCall = delta.getToolCalls().get(0);
            ToolExecutionRequestBuilder builder = this.toolExecutionBuilder.computeIfAbsent(
                    toolCall.getIndex(),
                    idx -> new ToolExecutionRequestBuilder());
            if (StrUtil.isNotEmpty(toolCall.getId())) {
                builder.idBuilder.append(toolCall.getId());
            }
            if (StrUtil.isNotEmpty(toolCall.getFunction().getName())) {
                builder.nameBuilder.append(toolCall.getFunction().getName());
            }
            if (StrUtil.isNotEmpty(toolCall.getFunction().getArguments())) {
                builder.argumentsBuilder.append(toolCall.getFunction().getArguments());
            }
        }
    }

    public CompletionResponse build() {
        AssistantMessage message = new AssistantMessage();
        message.setContent(this.contentBuilder.toString());
        message.setReasoningContent(this.reasonBuilder.toString());
        String refusal = this.refusalBuilder.toString();
        if (StrUtil.isNotEmpty(refusal)) {
            message.setRefusal(refusal);
        }
        if (!toolExecutionBuilder.isEmpty()) {
            List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
            toolExecutionBuilder.forEach((index, builder) -> {
                AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall();
                toolCall.setIndex(index);
                toolCall.setId(builder.idBuilder.toString());
                toolCall.setType("function");
                AssistantMessage.Function function = new AssistantMessage.Function();
                function.setName(builder.nameBuilder.toString());
                function.setArguments(builder.argumentsBuilder.toString());
                toolCall.setFunction(function);
                toolCalls.add(toolCall);
            });
            message.setToolCalls(toolCalls);
        }

        CompletionResponse response = new CompletionResponse();
        response.setId(this.id.get());
        response.setModel(this.model.get());
        response.setUsage(this.tokenUsage.get());
        CompletionResponse.Choices choices = new CompletionResponse.Choices();
        choices.setMessage(message);
        choices.setFinishReason(this.finishReason.get());
        response.setChoices(List.of(choices));

        return response;
    }

    private static class ToolExecutionRequestBuilder {
        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
