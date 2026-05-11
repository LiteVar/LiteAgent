package com.litevar.agent.rest.springai.audio;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.llm.TokenUsageService;
import com.litevar.agent.rest.util.AudioUtil;
import com.litevar.agent.rest.util.TikToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

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
    @Autowired
    private TokenUsageService tokenUsageService;

    private final Map<AiProvider, SpeechClient> speechClientMap = new ConcurrentHashMap<>();

    public SpeechService(List<SpeechClient> speechClients) {
        for (SpeechClient speechClient : speechClients) {
            speechClientMap.put(speechClient.getProvider(), speechClient);
        }
    }

    public byte[] speech(TokenReportDTO tokenReport, String modelId, String content) {
        LlmModel llmModel = modelService.findById(modelId);
        modelService.checkModelAvailable(llmModel.getId(), "");
        tokenUsageService.checkEnoughPoints(tokenReport.userId(), modelId);
        AiProvider provider = AiProvider.of(StrUtil.blankToDefault(llmModel.getProvider(), AiProvider.OPENAI.getValue()));

        byte[] bytes = speechClientMap
                .get(provider)
                .speech(llmModel, content);

        // 计算token usage
        String format = StrUtil.blankToDefault(llmModel.getResponseFormat(), provider == AiProvider.DASHSCOPE ? "mp3" : "pcm");
        int duration = AudioUtil.calculateDuration(bytes, format);
        log.info("Speech generation duration: {}s", duration);

        int inputTokens = TikToken.countTokens(content);
        // 以dashscope的 25tokens/s 计算
        int outputTokens = duration * 25;
        tokenUsageService.addUsage(tokenReport.userId(), modelId, tokenReport.agentId(), inputTokens, outputTokens);

        return bytes;
    }

    @SneakyThrows
    public Flux<String> speechStream(TokenReportDTO tokenReport, String modelId, String content) {
        LlmModel llmModel = modelService.findById(modelId);
        modelService.checkModelAvailable(llmModel.getId(), "");

        tokenUsageService.checkEnoughPoints(tokenReport.userId(), modelId);

        return speechClientMap
                .get(AiProvider.of(StrUtil.blankToDefault(llmModel.getProvider(), AiProvider.OPENAI.getValue())))
                .speechStream(tokenReport, llmModel, content);
    }

    public String transcribe(TokenReportDTO tokenReport, String modelId, MultipartFile audio) {
        LlmModel llmModel = modelService.findById(modelId);
        modelService.checkModelAvailable(llmModel.getId(), "");

        tokenUsageService.checkEnoughPoints(tokenReport.userId(), modelId);

        return speechClientMap
                .get(AiProvider.of(StrUtil.blankToDefault(llmModel.getProvider(), AiProvider.OPENAI.getValue())))
                .transcribe(tokenReport, llmModel, audio);
    }

    public Flux<String> transcribeStream(TokenReportDTO tokenReport, String modelId, MultipartFile audio) {
        LlmModel llmModel = modelService.findById(modelId);
        modelService.checkModelAvailable(llmModel.getId(), "");

        tokenUsageService.checkEnoughPoints(tokenReport.userId(), modelId);

        return speechClientMap
                .get(AiProvider.of(StrUtil.blankToDefault(llmModel.getProvider(), AiProvider.OPENAI.getValue())))
                .transcribeStream(tokenReport, llmModel, audio);
    }
}
