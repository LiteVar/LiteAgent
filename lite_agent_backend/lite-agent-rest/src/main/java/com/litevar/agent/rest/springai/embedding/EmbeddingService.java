package com.litevar.agent.rest.springai.embedding;


import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.llm.ModelService;
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

import java.util.Collections;
import java.util.List;

/**
 * @author reid
 * @since 2025/7/1
 */

@Service
public class EmbeddingService {
    @Autowired
    private ModelService modelService;

    public Embedding embedSegment(Document segment, String llmModelId) {
        return embed(Collections.singletonList(segment.getText()), llmModelId).get(0);
    }

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

        List<String> list = texts.stream().filter(StrUtil::isNotBlank).toList();

        EmbeddingModel embeddingModel = buildEmbeddingModel(modelId);
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(list, null));
        return response.getResults();
    }

    EmbeddingModel buildEmbeddingModel(String modelId) {
        LlmModel model = modelService.findById(modelId);
        if (model == null) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }

        return new OpenAiEmbeddingModel(
            OpenAiApi.builder()
                .baseUrl(StrUtil.removeSuffix(model.getBaseUrl(),  "/v1"))
                .apiKey(model.getApiKey())
                .build(),
            MetadataMode.EMBED,
            OpenAiEmbeddingOptions.builder().model(model.getName()).build()
        );
    }
}
