package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.rest.util.AudioUtil;
import lombok.SneakyThrows;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author reid
 * @since 2025/6/28
 */

@Service
public class SpeechService {
    @Autowired
    private ModelService modelService;

    private final Map<AiProvider, SpeechClient> speechClientMap = new ConcurrentHashMap<>();

    public SpeechService(List<SpeechClient> speechClients) {
        for (SpeechClient speechClient : speechClients) {
            speechClientMap.put(speechClient.getProvider(), speechClient);
        }
    }

    public byte[] speech(String modelId, String text) {
        LlmModel llmModel = modelService.findById(modelId);
        return speechClientMap
            .get(AiProvider.of(StrUtil.blankToDefault(llmModel.getProvider(), AiProvider.OPENAI.getValue())))
            .speech(llmModel, text);
    }

    @SneakyThrows
    public byte[] speechStream(String modelId, String content) {
        LlmModel llmModel = modelService.findById(modelId);

        Flux<DataBuffer> dataBufferFlux = speechClientMap
            .get(AiProvider.of(StrUtil.blankToDefault(llmModel.getProvider(), AiProvider.OPENAI.getValue())))
            .speechStream(llmModel, content)
            .map(trunk -> new DefaultDataBufferFactory().wrap(trunk));

        Mono<DataBuffer> joinedDataBuffer = DataBufferUtils.join(dataBufferFlux);

        Mono<String> finalFilePathMono = joinedDataBuffer.flatMap(fullDataBuffer -> {
            try {
                String finalFilePath;
                if ("pcm".equalsIgnoreCase(llmModel.getResponseFormat())) {
                    String pcmFilePath = AudioUtil.savePcmData(fullDataBuffer);
                    finalFilePath = AudioUtil.convertPcmToWav(pcmFilePath);
                    Files.deleteIfExists(Paths.get(pcmFilePath));
                } else {
                    String extension = llmModel.getResponseFormat().toLowerCase();
                    finalFilePath = AudioUtil.saveToWav(fullDataBuffer, extension);
                }
                // 释放 DataBuffer
                DataBufferUtils.release(fullDataBuffer);
                return Mono.just(finalFilePath);
            } catch (IOException e) {
                // 别忘了释放 DataBuffer，尤其是在异常路径上！
                DataBufferUtils.release(fullDataBuffer);
                return Mono.error(e);
            }
        });
        String finalFilePath = finalFilePathMono.block();
        byte[] resultBytes = Files.readAllBytes(Paths.get(finalFilePath));
        Files.deleteIfExists(Paths.get(finalFilePath));
        return resultBytes;
    }

    public String transcriptions(String modelId, MultipartFile audio) {
        LlmModel llmModel = modelService.findById(modelId);

        OpenAiAudioTranscriptionModel transcriptionModel = new OpenAiAudioTranscriptionModel(
            OpenAiAudioApi.builder()
                .baseUrl(StrUtil.removeSuffix(llmModel.getBaseUrl(), "/v1"))
                .apiKey(llmModel.getApiKey())
                .build(),
            OpenAiAudioTranscriptionOptions.builder()
                .model(llmModel.getName())
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .build()
        );

        AudioTranscriptionResponse response = transcriptionModel.call(new AudioTranscriptionPrompt(audio.getResource()));
        String output = response.getResult().getOutput();

        // 统一转换为JSON格式
        if (output.startsWith("{") && output.endsWith("}")) {
            // 已经是JSON格式,直接返回
            return output;
        } else {
            // 纯文本格式,转换为JSON
            return "{\"text\":\"" + output.replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
        }
    }
}
