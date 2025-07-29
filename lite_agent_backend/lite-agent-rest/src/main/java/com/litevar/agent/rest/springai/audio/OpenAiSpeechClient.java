package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
        return new OpenAiAudioSpeechModel(
            OpenAiAudioApi.builder()
                .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
                .apiKey(model.getApiKey())
                .build(),
            OpenAiAudioSpeechOptions.builder()
                .model(model.getName())
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.WAV)
                .build()
        );
    }
}
