package com.litevar.agent.openai;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.openai.completion.*;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author uncle
 * @since 2025/2/26 10:32
 */
@Slf4j
public class OpenAiChatModel {
    private static ChatContext messageContext;

    /**
     * stream=true
     */
    public static RequestExecutor.CallHandle<Void> generate(ChatModelRequest request, String taskId, List<Message> chatMessage,
                                                            CompletionCallback callback, TokenReportDTO tokenReport) {
        List<Message> allMessage = getMessage(chatMessage, request.getContextId(), taskId);

        CompletionRequestParam param = getParam(request, true, tokenReport);
        param.setMessages(allMessage);

        callback.start(taskId);

        CompletionStreamResponseBuilder responseBuilder = new CompletionStreamResponseBuilder();
        AtomicInteger thinkFlag = new AtomicInteger(0);
        return RequestExecutor.doStreamRequest(param, request.getBaseUrl(), request.getApiKey(), new RequestCallback() {
            @Override
            public void onResponse(String response) {
                try {
                    if (StrUtil.equals(response, "[DONE]")) {

                        CompletionResponse completionResponse = responseBuilder.build();
                        //report token usage
                        RequestExecutor.reportUsageIfPresent(tokenReport, completionResponse.getUsage());
                        afterResponse(completionResponse, chatMessage, request.getContextId(), taskId);
                        callback.onCompleteResponse(taskId, completionResponse);

                    } else {
                        CompletionResponse partialResponse = ObjectMapperSingleton.getObjectMapper().readValue(response, CompletionResponse.class);
                        //拼装返回结果片段
                        responseBuilder.append(partialResponse, thinkFlag);
                        for (CompletionResponse.Choices choice : partialResponse.getChoices()) {
                            String content = choice.getMessage().getContent();
                            if (StrUtil.isNotEmpty(content)) {
                                callback.onPartialResponse(taskId, content, 0);
                            }
                            String reasoningContent = choice.getMessage().getReasoningContent();
                            if (StrUtil.isNotEmpty(reasoningContent)) {
                                callback.onPartialResponse(taskId, reasoningContent, 1);
                            }
                        }
                    }
                } catch (Exception ex) {
                    callback.onError(taskId, ex);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onError(taskId, throwable);
            }
        });
    }

    /**
     * stream=false
     */
    public static RequestExecutor.CallHandle<CompletionResponse> generate(ChatModelRequest request, String taskId,
                                                                          List<Message> chatMessage, TokenReportDTO tokenReport) {
        List<Message> allMessage = getMessage(chatMessage, request.getContextId(), taskId);

        CompletionRequestParam param = getParam(request, false, tokenReport);
        param.setMessages(allMessage);

        return RequestExecutor.doRequestAsync(param, request.getBaseUrl(), request.getApiKey(), response -> afterResponse(response, chatMessage, request.getContextId(), taskId));
    }

    private static void afterResponse(CompletionResponse response, List<Message> chatMessage, String contextId, String taskId) {
        //将请求消息加入当前task上下文
        List<Message> toStore = new ArrayList<>(chatMessage);
        if (response.isFunctionCalling()) {
            response.getChoices().get(0).getMessage().getToolCalls().forEach(i -> {
                String arguments = i.getFunction().getArguments();
                if (StrUtil.isBlank(arguments.trim())) {
                    //兼容国产大模型function-calling argument参数为空返回空字符的情况
                    log.error("function-calling argument为空,将arguments修复为空括号");
                    i.getFunction().setArguments("{}");
                }
            });
        }
        CompletionResponse.Choices choice = response.getChoices().get(0);
        //返回结果加入上下文,function-calling的消息等返回结果再一起加入
        if (!response.isFunctionCalling() && choice.getMessage() != null) {
            toStore.add(choice.getMessage());
        }
        //将请求消息加入当前task上下文
        getMessageContext().addTaskMessage(contextId, taskId, toStore);
    }

    private static List<Message> getMessage(List<Message> chatMessage, String contextId, String taskId) {
        List<Message> allMessage = new ArrayList<>();
        //加入历史上下文消息
        List<Message> contextMsg = getMessageContext().getMessages(contextId);
        allMessage.addAll(contextMsg);

        //加入当前task上下文消息
        List<Message> taskMessages = getMessageContext().getTaskMessages(contextId, taskId);
        if (ObjectUtil.isNotEmpty(taskMessages)) {
            allMessage.addAll(taskMessages);
        }

        //加入当前请求消息
        allMessage.addAll(chatMessage);
        return allMessage;
    }

    private static CompletionRequestParam getParam(ChatModelRequest request, boolean stream, TokenReportDTO tokenReport) {
        CompletionRequestParam param = new CompletionRequestParam();
        param.setModel(request.getModel());
        param.setMaxCompletionTokens(request.getMaxCompletionTokens());
        param.setResponseFormat(request.getResponseFormat());
        param.setTemperature(request.getTemperature());
        param.setTopP(request.getTopP());
        param.setStream(stream);
        if (stream) {
            param.setStreamOptions(Map.of("include_usage", true));
        }
        param.setTools(request.getTools());
        param.setTokenReport(tokenReport);
        return param;
    }

    private static ChatContext getMessageContext() {
        if (messageContext == null) {
            messageContext = SpringBeanUtil.getBean(ChatContext.class);
        }
        return messageContext;
    }
}
