package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.core.module.llm.TokenUsageService;
import com.litevar.agent.rest.util.AudioUtil;
import com.litevar.agent.rest.util.TikToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Service
public class OpenAiSpeechClient implements SpeechClient {
    @Autowired
    private TokenUsageService tokenUsageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public byte[] speech(LlmModel model, String text) {
        return buildSpeechModel(model).call(text);
    }

    @Override
    public Flux<String> speechStream(TokenReportDTO tokenReport, LlmModel model, String text) {
        Map<String, String> requestBody = Map.of(
                "model", model.getName(),
                "stream_format", "sse",
                "voice", "alloy",
                "input", text,
                "response_format", StrUtil.blankToDefault(model.getResponseFormat(), "pcm"));

        return buildClient(model).post()
                .uri("/v1/audio/speech")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    if (line.startsWith("data:")) {
                        line = line.substring(5).trim();
                    }
                    if (StrUtil.isBlank(line) || "[DONE]".equals(line)) {
                        return Flux.empty();
                    }
                    return parseSpeechEvent(tokenReport.agentId(), model.getId(), tokenReport.userId(), line);
                });
    }

    @SneakyThrows
    @Override
    public String transcribe(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        String response = SpeechClient.super.transcribe(tokenReport, model, audio);

        int inputTokens;
        int outputTokens;
        if (JSONUtil.isTypeJSON(response)) {
            try {
                JsonNode node = objectMapper.readTree(response);
                JsonNode usage = node.path("usage");
                inputTokens = usage.get("input_tokens").asInt();
                outputTokens = usage.get("output_tokens").asInt();

                response = node.path("text").asText();
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        } else {
            // 普通字符串,没有返回usage信息,需要自己计算token
            inputTokens = AudioUtil.calculateDuration(audio.getBytes(), "wav") * 25;
            outputTokens = TikToken.countTokens(response);
        }

        tokenUsageService.addUsage(
                tokenReport.userId(), model.getId(), tokenReport.agentId(), inputTokens, outputTokens
        );

        return response;
    }

    @Override
    public Flux<String> transcribeStream(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        return SpeechClient.super.transcribeStream(tokenReport, model, audio)
                .flatMap(line -> parseTranscriptEvent(tokenReport, line));
    }

    private OpenAiAudioSpeechModel buildSpeechModel(LlmModel model) {
        // 解析响应格式，默认为 PCM
        OpenAiAudioApi.SpeechRequest.AudioResponseFormat responseFormat = OpenAiAudioApi.SpeechRequest.AudioResponseFormat.PCM;
        if (StrUtil.isNotBlank(model.getResponseFormat())) {
            try {
                responseFormat = OpenAiAudioApi.SpeechRequest.AudioResponseFormat
                    .valueOf(model.getResponseFormat().toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果格式不支持，使用默认的 WAV 格式
                responseFormat = OpenAiAudioApi.SpeechRequest.AudioResponseFormat.PCM;
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
                .build(),
                RETRY_TEMPLATE);
    }

    private Flux<String> parseTranscriptEvent(TokenReportDTO tokenReport, String raw) {
        String json = raw;
        if (json.startsWith("data:")) {
            json = json.substring(5).trim();
        }
        if (StrUtil.isBlank(json) || "[DONE]".equals(json)) {
            return Flux.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("type").asText();
            if (StrUtil.isBlank(type)) {
                String text = node.path("text").asText("");
                return StrUtil.isBlank(text) ? Flux.empty() : Flux.just(JSONUtil.toJsonStr(Map.of("text", text)));
            }
            if ("transcript.text.delta".equals(type)) {
                String delta = node.path("delta").asText("");
                return StrUtil.isBlank(delta) ? Flux.empty() : Flux.just(JSONUtil.toJsonStr(Map.of("text", delta)));
            }
            if ("transcript.text.done".equals(type)) {
                JsonNode usage = node.path("usage");
                if (!usage.isMissingNode() && !usage.isNull()) {
                    log.info("Transcription usage: {}", usage);
                    int inputTokens = usage.get("input_tokens").asInt();
                    int outputTokens = usage.get("output_tokens").asInt();
                    tokenUsageService.addUsage(
                            tokenReport.userId(), tokenReport.modelId(), tokenReport.agentId(), inputTokens, outputTokens);
                }
            }
            return Flux.empty();
        } catch (Exception e) {
            log.warn("Unable to parse transcript stream event: {}", json, e);
            return Flux.empty();
        }
    }

    private Flux<String> parseSpeechEvent(String agentId, String modelId, String userId, String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("type").asText();
            if ("speech.audio.delta".equals(type)) {
                String audio = node.path("audio").asText();
                if (StrUtil.isNotBlank(audio)) {
                    return Flux.just(JSONUtil.toJsonStr(Map.of("audio", audio)));
                }
            } else if ("speech.audio.done".equals(type)) {
                JsonNode usage = node.path("usage");
                if (!usage.isMissingNode()) {
                    log.info("Speech usage: {}", usage);
                    int inputTokens = usage.path("input_tokens").asInt();
                    int outputTokens = usage.path("output_tokens").asInt();
                    tokenUsageService.addUsage(
                        userId, modelId, agentId, inputTokens, outputTokens);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse speech event: {}", json, e);
        }
        return Flux.empty();
    }

}
