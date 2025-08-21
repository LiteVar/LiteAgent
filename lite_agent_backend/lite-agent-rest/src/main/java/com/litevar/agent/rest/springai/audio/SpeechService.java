package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.rest.util.AudioUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
                int dataSize = fullDataBuffer.readableByteCount();

                // 检查是否为音频数据还是错误响应
                byte[] headerBytes = new byte[Math.min(dataSize, 512)];
                fullDataBuffer.read(headerBytes, 0, headerBytes.length);

                // 重置buffer position以便后续读取
                fullDataBuffer.readPosition(0);

                // 检查是否为音频数据
                if (!isAudioData(headerBytes)) {
                    // 如果不是音频数据，读取全部内容作为错误信息
                    byte[] allBytes = new byte[dataSize];
                    fullDataBuffer.read(allBytes);
                    String errorContent = new String(allBytes, java.nio.charset.StandardCharsets.UTF_8);

                    DataBufferUtils.release(fullDataBuffer);
                    return Mono.error(new IOException(errorContent));
                }

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
                return Mono.error(new IOException("音频处理失败: " + e.getMessage(), e));
            }
        });

        try {
            String finalFilePath = finalFilePathMono.block();
            byte[] resultBytes = Files.readAllBytes(Paths.get(finalFilePath));
            Files.deleteIfExists(Paths.get(finalFilePath));
            return resultBytes;
        } catch (Exception e) {
            throw new RuntimeException("音频流处理失败: " + e.getMessage(), e);
        }
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

    /**
     * 检测数据是否为音频数据
     *
     * @param headerBytes 数据头部字节
     * @return true 如果是音频数据，false 如果是其他数据（如JSON错误响应）
     */
    private boolean isAudioData(byte[] headerBytes) {
        if (headerBytes == null || headerBytes.length < 4) {
            return false;
        }

        // 检查是否为JSON格式（错误响应通常是JSON）
        String headerStr = new String(headerBytes, 0, Math.min(headerBytes.length, 100), java.nio.charset.StandardCharsets.UTF_8);
        if (headerStr.trim().startsWith("{") || headerStr.trim().startsWith("[") ||
                headerStr.contains("\"error\"") || headerStr.contains("\"message\"")) {
            return false;
        }

        // 检查是否为HTML格式
        if (headerStr.toLowerCase().contains("<html") || headerStr.toLowerCase().contains("<!doctype")) {
            return false;
        }

        // 检查常见音频格式的魔数 (Magic Numbers)
        // WAV 格式：RIFF...WAVE
        if (headerBytes.length >= 12 &&
                headerBytes[0] == 'R' && headerBytes[1] == 'I' &&
                headerBytes[2] == 'F' && headerBytes[3] == 'F' &&
                headerBytes[8] == 'W' && headerBytes[9] == 'A' &&
                headerBytes[10] == 'V' && headerBytes[11] == 'E') {
            return true;
        }

        // MP3 格式：ID3 或 0xFF 0xFB
        if (headerBytes[0] == 'I' && headerBytes[1] == 'D' && headerBytes[2] == '3' || headerBytes[0] == (byte) 0xFF && (headerBytes[1] & 0xE0) == 0xE0) {
            return true;
        }

        // OGG 格式：OggS
        if (headerBytes[0] == 'O' && headerBytes[1] == 'g' && headerBytes[2] == 'g' && headerBytes[3] == 'S') {
            return true;
        }

        // FLAC 格式：fLaC
        if (headerBytes[0] == 'f' && headerBytes[1] == 'L' && headerBytes[2] == 'a' && headerBytes[3] == 'C') {
            return true;
        }

        // 如果前面的检查都没有匹配，但数据看起来是二进制数据（不是文本），则认为可能是音频
        // 检查是否包含大量不可打印字符（音频数据的特征）
        int nonPrintableCount = 0;
        int checkLength = Math.min(headerBytes.length, 100);
        for (int i = 0; i < checkLength; i++) {
            byte b = headerBytes[i];
            // 如果是控制字符但不是常见的空白字符，认为是二进制数据
            if (b < 32 && b != 9 && b != 10 && b != 13) {
                nonPrintableCount++;
            }
        }

        // 如果超过30%是不可打印字符，很可能是音频数据
        return (double) nonPrintableCount / checkLength > 0.3;
    }
}
