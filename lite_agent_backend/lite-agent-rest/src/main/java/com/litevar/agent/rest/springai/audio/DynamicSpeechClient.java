package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.llm.TokenUsageService;
import com.litevar.agent.rest.springai.http.DynamicBodyHttpConnector;
import com.litevar.agent.rest.util.AudioUtil;
import com.litevar.agent.rest.util.TikToken;
import com.litevar.agent.rest.util.TypewriterEffectUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * @author reid
 * @since 2025/6/28
 */
@Slf4j
@Service
public class DynamicSpeechClient implements SpeechClient {
    @Autowired
    private TokenUsageService tokenUsageService;

    @Override
    public AiProvider getProvider() {
        return AiProvider.OTHERS;
    }

    @Override
    public byte[] speech(LlmModel model, String text) {
        throw new ServiceException(ServiceExceptionEnum.SYSTEM_NOT_SUPPORT_MODEL);
    }

    @Override
    public Flux<String> speechStream(TokenReportDTO tokenReport, LlmModel model, String text) {
        return Flux.defer(() -> {
            ByteArrayOutputStream totalData = new ByteArrayOutputStream();
            return buildSpeechModel(model)
                    .stream(new TextToSpeechPrompt(text))
                    .map(response -> response.getResult().getOutput())
                    .map(output -> {
                        totalData.writeBytes(output);
                        return JSONUtil.toJsonStr(Map.of("audio", Base64.encode(output)));
                    })
                    .doOnComplete(() -> {
                        int duration = AudioUtil.calculateDuration(totalData.toByteArray(), model.getResponseFormat());
                        int inputTokens = TikToken.countTokens(text);
                        int outputTokens = duration * 25;
                        tokenUsageService.addUsage(
                                tokenReport.userId(), model.getId(), tokenReport.agentId(), inputTokens, outputTokens);
                    });
        });
    }

    @SneakyThrows
    @Override
    public String transcribe(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        String response = buildTranscriptionModel(model).transcribe(audio.getResource());
        String output = JSONUtil.parseObj(response).getStr("text");

        int inputTokens = AudioUtil.calculateDuration(audio.getBytes(), "wav") * 25;
        int outputTokens = TikToken.countTokens(output);
        tokenUsageService.addUsage(
            tokenReport.userId(), model.getId(), tokenReport.agentId(), inputTokens, outputTokens
        );

        return output;
    }

    @Override
    public Flux<String> transcribeStream(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        return Flux.<String>create(sink -> {
                try {
                    String text = transcribe(tokenReport, model, audio);
                    //这里接口不支持流式输出,拿到整段文字后,逐字输出
                    TypewriterEffectUtil.part(text, 50,
                        part -> sink.next(JSONUtil.toJsonStr(Map.of("text", part))));
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    @SneakyThrows
    private OpenAiAudioSpeechModel buildSpeechModel(LlmModel model) {
        return new OpenAiAudioSpeechModel(
            OpenAiAudioApi.builder()
                .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
                .apiKey(model.getApiKey())
                .webClientBuilder(
                    WebClient.builder().clientConnector(
                        DynamicBodyHttpConnector.buildConnector(new ObjectMapper().readValue(model.getFieldMapping(), new TypeReference<Map<String, String>>() {
                        }))
                    )
                )
                .build(),
            OpenAiAudioSpeechOptions.builder()
                .model(model.getName())
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.PCM)
                .build(),
            RETRY_TEMPLATE
        );
    }

}
