package com.litevar.agent.rest.springai.audio;


import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author reid
 * @since 2025/6/28
 */

@Service
public class DashScopeSpeechClient implements SpeechClient{
    @Override
    public AiProvider getProvider() {
        return AiProvider.DASHSCOPE;
    }

    @Override
    public byte[] speech(LlmModel model, String text) {
        SpeechSynthesisResponse response = buildModel(model).call(new SpeechSynthesisPrompt(text));
        return response.getResult().getOutput().getAudio().array();
    }

    @Override
    public Flux<byte[]> speechStream(LlmModel model, String text) {
        return buildModel(model).stream(new SpeechSynthesisPrompt(text))
            .flatMap(response -> {
                byte[] bytes = response.getResult().getOutput().getAudio().array();
                return Flux.just(bytes);
            });
    }

    private DashScopeSpeechSynthesisModel buildModel(LlmModel model) {
        return new DashScopeSpeechSynthesisModel(
            new DashScopeSpeechSynthesisApi(model.getApiKey()),
            DashScopeSpeechSynthesisOptions.builder()
                .model(model.getName())
                .responseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.WAV)
                .build()
        );
    }
}
