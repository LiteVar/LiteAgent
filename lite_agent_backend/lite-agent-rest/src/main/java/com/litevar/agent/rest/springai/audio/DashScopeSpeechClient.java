package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.llm.TokenUsageService;
import com.litevar.agent.rest.util.AudioUtil;
import com.litevar.agent.rest.util.TikToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author reid
 * @since 2025/6/28
 */
@Slf4j
@Service
public class DashScopeSpeechClient implements SpeechClient {
    @Autowired
    private TokenUsageService tokenUsageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiProvider getProvider() {
        return AiProvider.DASHSCOPE;
    }

    @Override
    public byte[] speech(LlmModel model, String text) {
        SpeechSynthesizer synthesizer = buildSpeechSynthesizer(model, null);
        try {
            return toBytes(synthesizer.call(text));
        } catch (Exception e) {
            throw new RuntimeException("DashScope speech failed", e);
        } finally {
            closeSynthesizer(synthesizer);
        }
    }

    @Override
    public Flux<String> speechStream(TokenReportDTO tokenReport, LlmModel model, String text) {
        return Flux.<String>create(sink -> {
                    CountDownLatch latch = new CountDownLatch(1);
                    List<byte[]> chunks = new java.util.ArrayList<>();
                    ResultCallback<SpeechSynthesisResult> callback = new ResultCallback<>() {
                        @Override
                        public void onEvent(SpeechSynthesisResult result) {
                            byte[] chunk = toBytes(result.getAudioFrame());
                            if (chunk.length == 0) {
                                return;
                            }
                            chunks.add(chunk);
                            sink.next(JSONUtil.toJsonStr(Map.of("audio", Base64.encode(chunk))));
                        }

                        @Override
                        public void onComplete() {
                            latch.countDown();
                        }

                        @Override
                        public void onError(Exception e) {
                            sink.error(e);
                            latch.countDown();
                        }
                    };
                    SpeechSynthesizer synthesizer = buildSpeechSynthesizer(model, callback);
                    try {
                        synthesizer.call(text);
                        latch.await();
                        int totalLength = chunks.stream().mapToInt(i -> i.length).sum();
                        byte[] all = new byte[totalLength];
                        int offset = 0;
                        for (byte[] item : chunks) {
                            System.arraycopy(item, 0, all, offset, item.length);
                            offset += item.length;
                        }
                        int duration = AudioUtil.calculateDuration(all, "mp3");
                        int inputTokens = TikToken.countTokens(text);
                        int outputTokens = duration * 25;
                        tokenUsageService.addUsage(tokenReport.userId(), model.getId(), tokenReport.agentId(), inputTokens, outputTokens);
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    } finally {
                        closeSynthesizer(synthesizer);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String transcribe(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        try {
            MultiModalConversationResult result = buildAsrClient(model).call(buildAsrParam(model, audio));
            String json = JsonUtils.toJson(result);
            JsonNode root = objectMapper.readTree(json);
            String text = parseAsrText(root);
            addUsage(tokenReport, model, audio, text, root.path("usage"));
            return text;
        } catch (Exception e) {
            throw new ServiceException("DashScope transcribe failed: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> transcribeStream(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio) {
        return Flux.<String>create(sink -> {
                    try {
                        StringBuilder totalText = new StringBuilder();
                        int[] promptTokens = new int[]{0};
                        int[] completionTokens = new int[]{0};
                        buildAsrClient(model).streamCall(buildAsrParam(model, audio)).blockingForEach(item -> {
                            JsonNode root = objectMapper.readTree(JsonUtils.toJson(item));
                            String text = parseAsrText(root);
                            if (StrUtil.isNotBlank(text)) {
                                totalText.append(text);
                                sink.next(JSONUtil.toJsonStr(Map.of("text", text)));
                            }
                            JsonNode usage = root.path("usage");
                            int prompt = usage.path("prompt_tokens").asInt(0);
                            int completion = usage.path("completion_tokens").asInt(0);
                            if (prompt > 0) {
                                promptTokens[0] = prompt;
                            }
                            if (completion > 0) {
                                completionTokens[0] = completion;
                            }
                        });
                        addUsage(tokenReport, model, audio, totalText.toString(), promptTokens[0], completionTokens[0]);
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private SpeechSynthesizer buildSpeechSynthesizer(LlmModel model, ResultCallback<SpeechSynthesisResult> callback) {
        Constants.baseWebsocketApiUrl = resolveWsBaseUrl(model);
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(model.getApiKey())
                .model(model.getName())
                .voice(resolveVoice(model))
                .build();
        return new SpeechSynthesizer(param, callback);
    }

    private void closeSynthesizer(SpeechSynthesizer synthesizer) {
        if (synthesizer == null) {
            return;
        }
        try {
            synthesizer.getDuplexApi().close(1000, "bye");
        } catch (Exception e) {
            log.warn("Close SpeechSynthesizer websocket failed", e);
        }
    }

    private MultiModalConversation buildAsrClient(LlmModel model) {
        Constants.baseHttpApiUrl = resolveHttpBaseUrl(model);
        return new MultiModalConversation();
    }

    private MultiModalConversationParam buildAsrParam(LlmModel model, MultipartFile audio) {
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(Collections.singletonMap("audio", buildDataUrl(audio))))
                .build();
        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("enable_itn", false);
        return MultiModalConversationParam.builder()
                .apiKey(model.getApiKey())
                .model(model.getName())
                .message(userMessage)
                .parameter("asr_options", asrOptions)
                .build();
    }

    private String parseAsrText(JsonNode root) {
        String text = root.path("output").path("choices").path(0).path("message").path("content").path(0).path("text").asText("");
        if (StrUtil.isNotBlank(text)) {
            return text;
        }
        String delta = root.path("output").path("choices").path(0).path("delta").path("content").asText("");
        if (StrUtil.isNotBlank(delta)) {
            return delta;
        }
        return root.path("output").path("text").asText("");
    }

    private String resolveHttpBaseUrl(LlmModel model) {
        String baseUrl = StrUtil.blankToDefault(model.getBaseUrl(), "https://dashscope.aliyuncs.com/api/v1");
        if (StrUtil.contains(baseUrl, "/compatible-mode/")) {
            baseUrl = baseUrl.substring(0, baseUrl.indexOf("/compatible-mode/"));
            baseUrl = StrUtil.removeSuffix(baseUrl, "/") + "/api/v1";
        }
        if (!StrUtil.contains(baseUrl, "/api/v1")) {
            baseUrl = StrUtil.removeSuffix(baseUrl, "/") + "/api/v1";
        }
        return StrUtil.removeSuffix(baseUrl, "/services/aigc/multimodal-generation/generation");
    }

    private String resolveWsBaseUrl(LlmModel model) {
        String baseUrl = StrUtil.blankToDefault(model.getBaseUrl(), "https://dashscope.aliyuncs.com/api/v1");
        if (StrUtil.contains(baseUrl, "dashscope-intl.aliyuncs.com")) {
            return "wss://dashscope-intl.aliyuncs.com/api-ws/v1/inference";
        }
        if (StrUtil.contains(baseUrl, "dashscope-us.aliyuncs.com")) {
            return "wss://dashscope-us.aliyuncs.com/api-ws/v1/inference";
        }
        return "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
    }

    private String resolveVoice(LlmModel model) {
        if (StrUtil.isNotBlank(model.getFieldMapping()) && JSONUtil.isTypeJSON(model.getFieldMapping())) {
            String voice = JSONUtil.parseObj(model.getFieldMapping()).getStr("voice");
            if (StrUtil.isNotBlank(voice)) {
                return voice;
            }
        }
        if (StrUtil.containsIgnoreCase(model.getName(), "cosyvoice-v3")) {
            return "longanyang";
        }
        if (StrUtil.containsIgnoreCase(model.getName(), "cosyvoice-v2")) {
            return "longxiaochun_v2";
        }
        return "longhua";
    }

    private void addUsage(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio, String output, JsonNode usage) {
        int inputTokens = usage.path("prompt_tokens").asInt(0);
        int outputTokens = usage.path("completion_tokens").asInt(0);
        addUsage(tokenReport, model, audio, output, inputTokens, outputTokens);
    }

    private void addUsage(TokenReportDTO tokenReport, LlmModel model, MultipartFile audio, String output, int inputTokens,
                          int outputTokens) {
        if (inputTokens <= 0) {
            inputTokens = AudioUtil.calculateDuration(getAudioBytes(audio), "wav") * 25;
        }
        if (outputTokens <= 0) {
            outputTokens = TikToken.countTokens(output);
        }
        tokenUsageService.addUsage(tokenReport.userId(), model.getId(), tokenReport.agentId(), inputTokens, outputTokens);
    }

    @SneakyThrows
    private byte[] toBytes(ByteBuffer buffer) {
        if (buffer == null) {
            return new byte[0];
        }
        ByteBuffer copy = buffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return bytes;
    }

    @SneakyThrows
    private String buildDataUrl(MultipartFile audio) {
        return "data:" + resolveAudioMimeType(audio) + ";base64," + Base64.encode(audio.getBytes());
    }

    private String resolveAudioMimeType(MultipartFile audio) {
        String extension = StrUtil.blankToDefault(audio.getOriginalFilename(), "");
        extension = extension.contains(".") ? extension.substring(extension.lastIndexOf('.') + 1) : extension;
        extension = extension.toLowerCase();
        return switch (extension) {
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "m4a", "mp4" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "amr" -> "audio/amr";
            case "flac" -> "audio/flac";
            case "ogg", "opus" -> "audio/ogg";
            case "webm" -> "audio/webm";
            case "mka", "mkv" -> "audio/x-matroska";
            default -> StrUtil.blankToDefault(audio.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        };
    }

    @SneakyThrows
    private byte[] getAudioBytes(MultipartFile audio) {
        return audio.getBytes();
    }

}
