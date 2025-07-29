package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.rest.springai.http.DynamicBodyHttpConnector;
import lombok.SneakyThrows;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author reid
 * @since 2025/6/28
 */

@Service
public class DynamicSpeechClient implements SpeechClient{
    @Override
    public AiProvider getProvider() {
        return AiProvider.OTHERS;
    }

    @Override
    public byte[] speech(LlmModel model, String text) {
        return buildModel(model).call(text);
    }

    @Override
    public Flux<byte[]> speechStream(LlmModel model, String text) {
        return buildModel(model).stream(text);
    }

    @SneakyThrows
    private OpenAiAudioSpeechModel buildModel(LlmModel model) {
        return new OpenAiAudioSpeechModel(
            OpenAiAudioApi.builder()
                .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
                .apiKey(model.getApiKey())
                .webClientBuilder(
                    WebClient.builder().clientConnector(
                        DynamicBodyHttpConnector.buildConnector(new ObjectMapper().readValue(model.getFieldMapping(), new TypeReference<Map<String, String>>() {}))
                    )
                )
                .build(),
            OpenAiAudioSpeechOptions.builder()
                .model(model.getName())
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.WAV)
                .build()
        );
    }
}
