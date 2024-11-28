package com.litevar.agent.rest.langchain4j;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.AgentSendMsgDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import com.litevar.agent.rest.langchain4j.handler.AiMessageHandler;
import com.litevar.agent.rest.langchain4j.handler.LogMessageHandler;
import com.litevar.agent.rest.langchain4j.handler.SseClientMessageHandler;
import com.litevar.agent.rest.langchain4j.handler.StoreMessageHandler;
import com.litevar.agent.rest.util.RedisChatMemoryStore;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author uncle
 * @since 2024/10/15 15:01
 */
@Slf4j
public class AiClientManager {
    private static final Map<String, SessionInfo> clientMap = new ConcurrentHashMap<>();
    private static final ChatMemoryStore chatMemoryStore = new RedisChatMemoryStore();

    public static String initSession(LlmModel model, List<ToolFunction> functionList, String prompt,
                                     Double temperature, Double topP, Integer maxTokens) {
        StreamingChatLanguageModel languageModel = AiStreamClient.buildModel(model, temperature, topP, maxTokens);

        List<ToolSpecification> invokeToolList = AiStreamClient.buildTool(functionList);

        AiStreamClient client = AiStreamClient.getInstance(languageModel, prompt, invokeToolList, chatMemoryStore);
        log.info("init success,sessionId={}", client.getMemoryId());

        SessionInfo sessionInfo = new SessionInfo();
        clientMap.put(client.getMemoryId(), sessionInfo);
        sessionInfo.setClient(client);
        LogMessageHandler logHandler = new LogMessageHandler(client.getMemoryId());
        sessionInfo.getHandler().put(logHandler.role(), logHandler);
        StoreMessageHandler storeHandler = new StoreMessageHandler(client.getMemoryId());
        sessionInfo.getHandler().put(storeHandler.role(), storeHandler);
        return client.getMemoryId();
    }

    public static void clearSession(String sessionId) {
        SessionInfo sessionInfo = clientMap.get(sessionId);
        if (sessionInfo != null) {
            sessionInfo.getClient().clear();
            sessionInfo.getHandler().clear();
            clientMap.remove(sessionId);
            log.info("clear session success,sessionId={}", sessionId);
        }
    }

    @PostConstruct
    public void init() {
        Executors.newScheduledThreadPool(5)
                .scheduleWithFixedDelay(() -> clientMap.forEach((id, client) -> {
                    Object value = RedisUtil.getValue(String.format(CacheKey.SESSION_INFO, id));
                    if (value == null) {
                        clearSession(id);
                    }
                }), 3, 2, TimeUnit.MINUTES);
    }

    private static void chat(String memoryId, String message, StreamMessageListener listener) {
        SessionInfo sessionInfo = clientMap.get(memoryId);
        if (sessionInfo == null || sessionInfo.getClient() == null) {
            throw new ServiceException(ServiceExceptionEnum.INIT_SESSION_FIRST);
        } else {
            sessionInfo.getClient().chat(message, listener);
        }
    }

    public static void chat(String sessionId, List<AgentSendMsgDTO> dto, SseClientMessageHandler sseMessageHandler) {
        SessionInfo sessionInfo = clientMap.get(sessionId);
        sessionInfo.getHandler().put(sseMessageHandler.role(), sseMessageHandler);

        String text = dto.get(0).getMessage();
        OutMessage userMessage = handleUserMessage(text);

        Collection<AiMessageHandler> handler = sessionInfo.getHandler().values();
        handler.forEach(i -> i.onSend(userMessage));

        chat(sessionId, text, new StreamMessageListener() {
            @Override
            public void onComplete(Response<AiMessage> message) {
                handler.forEach(i -> i.onComplete(message));
                sessionInfo.getHandler().remove(SseClientMessageHandler.ROLE);
            }

            @Override
            public void onError(Throwable e) {
                handler.forEach(i -> i.onError(e));
                sessionInfo.getHandler().remove(SseClientMessageHandler.ROLE);
            }

            @Override
            public void onNext(String part) {
                handler.forEach(i -> i.onNext(part));
            }

            @Override
            public String callFunction(ToolExecutionRequest toolExecutionRequest) {
                OutMessage toolRequestMessage = handleToolRequestMessage(toolExecutionRequest);
                handler.forEach(i -> i.callFunction(toolRequestMessage));

                //调用函数接口
                String[] arr = toolExecutionRequest.name().split("_");
                String functionId = arr[arr.length - 1];
                String res = AiClientManager.callFunction(toolRequestMessage.getToolCalls().get(0).getArguments(), sessionId, functionId);

                OutMessage resultMessage = handleToolResultMessage(res, toolExecutionRequest.id());
                handler.forEach(i -> i.functionResult(resultMessage));

                return res;
            }
        });
    }

    private static OutMessage handleUserMessage(String text) {
        OutMessage outMessage = new OutMessage();
        outMessage.setRole("user");
        outMessage.setType("text");
        outMessage.setContent(text);
        return outMessage;
    }

    private static OutMessage handleToolRequestMessage(ToolExecutionRequest request) {
        JSONObject argument = JSONUtil.parseObj(request.arguments());
        OutMessage.FunctionCall functionCall = new OutMessage.FunctionCall();
        functionCall.setId(request.id());
        functionCall.setName(request.name());
        functionCall.setArguments(argument);

        OutMessage functionCallMessage = new OutMessage();
        functionCallMessage.setRole("assistant");
        functionCallMessage.setType("functionCallList");
        functionCallMessage.setToolCalls(List.of(functionCall));

        return functionCallMessage;
    }

    private static String callFunction(JSONObject argument, String sessionId, String functionId) {
        ToolFunction function = (ToolFunction) RedisUtil.getValue(String.format(CacheKey.SESSION_FUNCTION_INFO, sessionId, functionId));
        //调用接口获取结果
        FunctionExecutor executor = ToolHandleFactory.getFunctionExecutor(function.getProtocol());
        String value = (String) RedisUtil.getValue(String.format(CacheKey.TOOL_API_KEY, sessionId, function.getToolId()));
        Map<String, String> defineHeader = new HashMap<>();
        if (StrUtil.isNotEmpty(value)) {
            defineHeader.put(HttpHeaders.AUTHORIZATION, value);
        }
        //tool 调用返回结果
        String res = "";
        try {
            res = executor.invoke(function, argument, defineHeader);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
            res = "tool调用失败";
        }
        return res;
    }

    private static OutMessage handleToolResultMessage(String res, String requestId) {
        OutMessage toolResultMessage = new OutMessage();
        toolResultMessage.setType("tool");
        toolResultMessage.setContent(res);
        toolResultMessage.setType("toolReturn");
        toolResultMessage.setToolCallId(requestId);
        return toolResultMessage;
    }

    @Getter
    @Setter
    public static class SessionInfo {
        private AiStreamClient client;
        private final Map<String, AiMessageHandler> handler = new ConcurrentHashMap<>();
    }
}