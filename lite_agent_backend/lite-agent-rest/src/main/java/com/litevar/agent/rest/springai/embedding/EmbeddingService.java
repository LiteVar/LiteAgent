package com.litevar.agent.rest.springai.embedding;


import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.util.LlmContext;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.llm.TokenUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author reid
 * @since 2025/7/1
 */
@Slf4j
@Service
public class EmbeddingService {
    private static final int MAX_BATCH_CHAR_COUNT = 20_000;

    @Autowired
    private ModelService modelService;
    @Autowired
    private TokenUsageService tokenUsageService;

    public List<Embedding> embedSegments(List<Document> segments, String llmModelId) {
        return embed(segments.stream().map(Document::getText).toList(), llmModelId);
    }

    public Embedding embedText(String text, String llmModelId) {
        return embed(Collections.singletonList(text), llmModelId).get(0);
    }

    public List<Embedding> embed(List<String> texts, String modelId) {
        if (texts.isEmpty()) {
            return Collections.emptyList();
        }
        String userId;
        String agentId = LlmContext.getAgentId();
        try {
            userId = LoginContext.currentUserId();
        } catch (Exception e) {
            userId = LlmContext.getUserId();
        }

        EmbeddingModel embeddingModel = buildEmbeddingModel(modelId, userId);
        List<List<String>> batches = partition(texts);
        log.info("开始embedding数据，总数:{}，批次:{}", texts.size(), batches.size());

        int promptTokens = 0;
        int completionTokens = 0;

        List<Embedding> embeddings = new ArrayList<>(texts.size());
        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            int batchCharCount = batch.stream().mapToInt(String::length).sum();
            log.info("开始embedding第{}批，数量:{}, 字符总数:{}", i + 1, batch.size(), batchCharCount);
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(batch, null));
            log.info("结束embedding第{}批", i + 1);
            embeddings.addAll(response.getResults());

            promptTokens += response.getMetadata().getUsage().getPromptTokens();
            completionTokens += response.getMetadata().getUsage().getCompletionTokens();
        }

        tokenUsageService.addUsage(userId, modelId, agentId, promptTokens, completionTokens);
        return embeddings;
    }

    EmbeddingModel buildEmbeddingModel(String modelId, String userId) {
        LlmModel model = modelService.findById(modelId);
        modelService.checkModelAvailable(model.getId(), "");

        if (model.getWorkspaceId().equalsIgnoreCase("0")) {
            //系统级模型,判断用户积分余额
            tokenUsageService.checkEnoughPoints(userId, modelId);
        }

        log.info("向量模型:{},url:{}", model.getName(), model.getBaseUrl());
        return new OpenAiEmbeddingModel(
                OpenAiApi.builder()
                        .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(), "/v1"))
                        .apiKey(model.getApiKey())
                        .build(),
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(model.getName()).build()
        );
    }

    private List<List<String>> partition(List<String> source) {
        List<List<String>> batches = new ArrayList<>();
        List<String> currentBatch = new ArrayList<>();
        int curBatchLength = 0;

        for (String segment : source) {
            if (segment.length() >= MAX_BATCH_CHAR_COUNT) {
                if (!currentBatch.isEmpty()) {
                    batches.add(currentBatch);
                    currentBatch = new ArrayList<>();
                    curBatchLength = 0;
                }
                batches.add(List.of(segment));
                continue;
            }
            if (curBatchLength + segment.length() >= MAX_BATCH_CHAR_COUNT) {
                if (!currentBatch.isEmpty()) {
                    batches.add(currentBatch);
                }
                currentBatch = new ArrayList<>();
                curBatchLength = 0;
            }

            currentBatch.add(segment);
            curBatchLength += segment.length();
        }
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        return batches;
    }
}
