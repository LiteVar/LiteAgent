package com.litevar.agent.openai;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.openai.completion.*;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.util.JsonRepair;
import com.litevar.agent.openai.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author uncle
 * @since 2025/2/26 10:32
 */
@Slf4j
public class OpenAiChatModel {
    private static ChatContext messageContext;
    private static final Pattern thinkPattern = Pattern.compile("<think>(.*?)</think>");

    /**
     * stream=true
     */
    public static void generate(ChatModelRequest request, String taskId, List<Message> chatMessage, CompletionCallback callback) {
        List<Message> allMessage = getMessage(chatMessage, request.getContextId(), taskId);

        CompletionRequestParam param = getParam(request, true);
        param.setMessages(allMessage);

        callback.start(taskId);

        CompletionStreamResponseBuilder responseBuilder = new CompletionStreamResponseBuilder();
        AtomicInteger thinkFlag = new AtomicInteger(0);
        RequestExecutor.doStreamRequest(param, request.getBaseUrl(), request.getApiKey(), new RequestCallback() {
            @Override
            public void onResponse(String response) {
                try {
                    if (StrUtil.equals(response, "[DONE]")) {

                        CompletionResponse completionResponse = responseBuilder.build();
                        afterResponse(completionResponse, chatMessage, request.getContextId(), taskId);
                        callback.onCompleteResponse(taskId, completionResponse, true);

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
    public static CompletionResponse generate(ChatModelRequest request, String taskId, List<Message> chatMessage) {
        List<Message> allMessage = getMessage(chatMessage, request.getContextId(), taskId);

        CompletionRequestParam param = getParam(request, false);
        param.setMessages(allMessage);

        CompletionResponse response = RequestExecutor.doRequest(param, request.getBaseUrl(), request.getApiKey());
        AssistantMessage assistantMessage = response.getChoices().get(0).getMessage();
        if (StrUtil.isNotEmpty(assistantMessage.getContent())) {
            String content = assistantMessage.getContent();
            Matcher matcher = thinkPattern.matcher(content);
            if (matcher.find()) {
                //content字段包含<think></think>的情况
                assistantMessage.setReasoningContent(matcher.group(1));
                assistantMessage.setContent(content.replaceAll("<think>.*?</think>", ""));
            }
        }
        afterResponse(response, chatMessage, request.getContextId(), taskId);
        return response;
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
                } else {
                    try {
                        validateJson(arguments);
                    } catch (Exception e) {
                        log.error("function-calling json解析异常,将对字符串进行修复:{}", arguments);
                        try {
                            String repairStr = JsonRepair.jsonrepair(arguments);
                            log.info("修复完成:{}", repairStr);
                            validateJson(repairStr);
                            log.info("解析成功,将替换响应数据");
                            i.getFunction().setArguments(repairStr);
                        } catch (Exception ex) {
                            log.error("修复失败", ex);
                        }
                    }
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

    private static CompletionRequestParam getParam(ChatModelRequest request, boolean stream) {
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
        return param;
    }

    private static ChatContext getMessageContext() {
        if (messageContext == null) {
            messageContext = SpringBeanUtil.getBean(ChatContext.class);
        }
        return messageContext;
    }

    private static void validateJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperSingleton.getObjectMapper();
        JsonFactory factory = mapper.getFactory();
        try (JsonParser parser = factory.createParser(json)) {
            parser.nextToken();
            mapper.readTree(parser);

            //检查是否还有多余的token
            if (parser.nextToken() != null) {
                throw new JsonProcessingException("JSON格式错误：包含多个根元素") {
                };
            }
        } catch (IOException e) {
            throw new JsonProcessingException("JSON格式错误：" + e.getMessage()) {
            };
        }
    }
}
