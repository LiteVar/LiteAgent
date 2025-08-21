package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class OpenAiSpeechClient implements SpeechClient {
    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public byte[] speech(LlmModel model, String text) {
        return buildModel(model).call(text);
    }

    @Override
    public Flux<byte[]> speechStream(LlmModel model, String text) {
        return buildModel(model).stream(text);
    }

    private OpenAiAudioSpeechModel buildModel(LlmModel model) {
        // 解析响应格式，默认为 WAV
        OpenAiAudioApi.SpeechRequest.AudioResponseFormat responseFormat = OpenAiAudioApi.SpeechRequest.AudioResponseFormat.WAV;
        if (StrUtil.isNotBlank(model.getResponseFormat())) {
            try {
                responseFormat = OpenAiAudioApi.SpeechRequest.AudioResponseFormat.valueOf(model.getResponseFormat().toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果格式不支持，使用默认的 WAV 格式
                responseFormat = OpenAiAudioApi.SpeechRequest.AudioResponseFormat.WAV;
            }
        }
        
        return new OpenAiAudioSpeechModel(
            OpenAiAudioApi.builder()
                .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
                .apiKey(model.getApiKey())
                .build(),
            OpenAiAudioSpeechOptions.builder()
                .model(model.getName())
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(responseFormat)
                .build()
        );
    }
}
