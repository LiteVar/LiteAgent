package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public interface SpeechClient {
    RetryTemplate RETRY_TEMPLATE = createRetryTemplate();
    AiProvider getProvider();

    byte[] speech(LlmModel model, String text);

    Flux<String> speechStream(TokenReportDTO tokenReport, LlmModel model, String text);

    default String transcribe(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("model", model.getName());
        bodyBuilder.part("response_format", StrUtil.blankToDefault(model.getResponseFormat(), "json").toLowerCase());
        bodyBuilder.part("file", audio.getResource())
                .filename(StrUtil.blankToDefault(audio.getOriginalFilename(), "audio"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return buildClient(model).post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).map(IllegalStateException::new)
                )
                .bodyToMono(String.class)
                .block();
    }

    /**
     * 默认实现OpenAI音频流式转录
     *
     * @param model
     * @param audio
     * @return
     */
    default Flux<String> transcribeStream(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("model", model.getName());
        bodyBuilder.part("stream", "true");
        bodyBuilder.part("response_format", StrUtil.blankToDefault(model.getResponseFormat(), "json").toLowerCase());
        bodyBuilder.part("file", audio.getResource())
                .filename(StrUtil.blankToDefault(audio.getOriginalFilename(), "audio"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return buildClient(model).post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .flatMapMany(body -> Flux.error(new IllegalStateException(body)));
                    }

                    MediaType contentType = response.headers().contentType().orElse(null);
                    if (contentType != null && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)) {
                        return response.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                                })
                                .flatMap(event -> {
                                    String data = event.data();
                                    return data == null ? Flux.empty() : Flux.just(data);
                                });
                    }

                    return response.bodyToMono(String.class).flux();
                });
    }

    default WebClient buildClient(LlmModel model) {
        return WebClient.builder()
            .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
            .defaultHeader("Authorization", "Bearer " + model.getApiKey())
            .build();
    }

    default TranscriptionModel buildTranscriptionModel(LlmModel model) {
        return new OpenAiAudioTranscriptionModel(
            OpenAiAudioApi.builder()
                .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
                .apiKey(model.getApiKey())
                .build(),
            OpenAiAudioTranscriptionOptions.builder()
                .model(model.getName())
                .responseFormat(
                    OpenAiAudioApi.TranscriptResponseFormat.valueOf(
                        StrUtil.blankToDefault(model.getResponseFormat().toUpperCase(), "JSON")
                    )
                )
                .build(),
            RETRY_TEMPLATE
        );
    }

    private static RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
