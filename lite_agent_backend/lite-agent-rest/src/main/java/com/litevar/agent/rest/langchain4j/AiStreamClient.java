package com.litevar.agent.rest.langchain4j;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.entity.ToolFunction;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装调用AI客户端
 *
 * @author uncle
 * @since 2024/10/15 15:01
 */
@Slf4j
public class AiStreamClient {
    private Assistant assistant;
    @Getter
    private String memoryId;

    private StreamMessageListener listener;

    private StreamingChatLanguageModel model;
    private List<ToolSpecification> invokeToolList;
    private String prompt;
    private ChatMemoryStore chatMemoryStore;
    private ToolExecutor toolExecutor;

    private AiStreamClient() {
    }

    public static AiStreamClient getInstance(StreamingChatLanguageModel model, String prompt,
                                             List<ToolSpecification> invokeToolList, ChatMemoryStore chatMemoryStore) {

        Assert.notNull(model, "model must not be null");
        Assert.notNull(chatMemoryStore, "chatMemoryStore must not be null");
        AiStreamClient client = new AiStreamClient();
        client.model = model;
        client.invokeToolList = invokeToolList;
        client.prompt = prompt;
        client.chatMemoryStore = chatMemoryStore;

        client.init();

        return client;
    }

    public static StreamingChatLanguageModel buildModel(LlmModel model, Double temperature, Double topP, Integer maxTokens) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder modelBuilder = OpenAiStreamingChatModel.builder()
                .baseUrl(model.getBaseUrl() + "/v1")
                .apiKey(model.getApiKey())
                .modelName(model.getName())
                .timeout(Duration.ofSeconds(120));
        if (ObjectUtil.isNotEmpty(temperature)) {
            modelBuilder.temperature(temperature);
        }
        if (ObjectUtil.isNotEmpty(topP)) {
            modelBuilder.topP(topP);
        }
        if (ObjectUtil.isNotEmpty(maxTokens)) {
            modelBuilder.maxTokens(maxTokens);
        }
        log.info("[initSession model] url:{},apiKey:{},name:{},temperature:{},topP:{},maxTokens:{}",
                model.getBaseUrl(), model.getApiKey(), model.getName(), temperature, topP, maxTokens);
        return modelBuilder.build();
    }

    public static List<ToolSpecification> buildTool(List<ToolFunction> functionList) {
        List<ToolSpecification> invokeToolList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(functionList)) {
            functionList.forEach(function -> {
                String name = function.getResource().replace("/", "_").replace(" ", "") + "_" + function.getId();

                ToolSpecification.Builder builder = ToolSpecification.builder()
                        //名字由functionName+dbId组成
                        .name(name)
                        .description(function.getDescription());

                if (!function.getParameters().isEmpty()) {
                    ToolParameters parameters = ToolParameters.builder().build();

                    for (ToolFunction.ParameterInfo functionParam : function.getParameters()) {
                        if (functionParam.isRequired()) {
                            parameters.required().add(functionParam.getParamName());
                        }
                        Map<String, Object> paramInfo = travelField(functionParam);
                        parameters.properties().put(functionParam.getParamName(), paramInfo);
                    }
                    builder.parameters(parameters);
                }

                ToolSpecification specification = builder.build();
                invokeToolList.add(specification);
                log.info("[initSession function] {}", specification);
            });
        }
        return invokeToolList;
    }

    private static Map<String, Object> travelField(ToolFunction.ParameterInfo param) {
        Map<String, Object> info = new HashMap<>();
        info.put("description", param.getDescription());
        String type = param.getType();
        info.put("type", type);
        if (ObjectUtil.isNotEmpty(param.getDefaultValue())) {
            info.put("default", param.getDefaultValue());
        }
        if (!param.getEnums().isEmpty()) {
            info.put("enum", param.getEnums());
        }

        if (StrUtil.equals(type, "object")) {
            Map<String, Object> subInfo = new HashMap<>();
            List<String> subRequired = new ArrayList<>();
            for (ToolFunction.ParameterInfo subParam : param.getProperties()) {
                subInfo.put(subParam.getParamName(), travelField(subParam));
                if (subParam.isRequired()) {
                    subRequired.add(subParam.getParamName());
                }
            }
            info.put("properties", subInfo);
            info.put("required", subRequired);

        } else if (StrUtil.equals(type, "array")) {
            ToolFunction.ParameterInfo item = param.getProperties().get(0);
            String subType = item.getType();
            Map<String, Object> itemProperties = new HashMap<>();
            itemProperties.put("type", subType);
            itemProperties.put("description", item.getDescription());

            if (StrUtil.equals(subType, "object")) {

                Map<String, Object> subInfo = new HashMap<>();
                List<String> subRequired = new ArrayList<>();
                for (ToolFunction.ParameterInfo subParam : item.getProperties()) {
                    if (subParam.isRequired()) {
                        subRequired.add(subParam.getParamName());
                    }
                    subInfo.put(subParam.getParamName(), travelField(subParam));
                }
                itemProperties.put("required", subRequired);
                itemProperties.put("properties", subInfo);
            }

            info.put("items", itemProperties);

        }

        return info;
    }

    private void init() {
        this.memoryId = IdUtil.fastSimpleUUID();
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryStore(this.chatMemoryStore)
                .id(this.memoryId)
                .maxMessages(20).build();

        AiServices<Assistant> serviceBuilder = AiServices.builder(Assistant.class)
                .streamingChatLanguageModel(this.model)
                .chatMemory(chatMemory);
        if (StrUtil.isNotBlank(this.prompt)) {
            serviceBuilder.systemMessageProvider(p -> this.prompt);
        }
        if (ObjectUtil.isNotEmpty(this.invokeToolList)) {
            this.toolExecutor = (request, memoryId) -> {
                //调用tool获取结果返回给AI
                if (AiStreamClient.this.listener != null) {
                    return AiStreamClient.this.listener.callFunction(request);
                }

                return "调用失败";
            };
            Map<ToolSpecification, ToolExecutor> map = new HashMap<>();
            for (ToolSpecification toolSpecification : this.invokeToolList) {
                map.put(toolSpecification, this.toolExecutor);
            }
            serviceBuilder.tools(map);
        }

        this.assistant = serviceBuilder.build();
    }

    public void chat(String message, StreamMessageListener listener) {
        this.listener = listener;
        TokenStream tokenStream = this.assistant.chat(message);
        tokenStream.onNext(t -> {
                    if (AiStreamClient.this.listener != null) {
                        AiStreamClient.this.listener.onNext(t);
                    }
                })
                .onComplete(aiMessageResponse -> {
                    if (AiStreamClient.this.listener != null) {
                        AiStreamClient.this.listener.onComplete(aiMessageResponse);
                    }
                    AiStreamClient.this.listener = null;
                })
                .onError(ex -> {
                    ex.printStackTrace();
                    AiStreamClient.this.listener.onError(ex);
                    AiStreamClient.this.listener = null;
                })
                .onRetrieved(System.out::println)
                .start();
    }

    public void clear() {
        this.toolExecutor = null;
        this.invokeToolList.clear();
        this.model = null;
        this.listener = null;
        this.assistant = null;
    }
}
