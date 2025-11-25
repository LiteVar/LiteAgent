package com.litevar.agent.rest.service;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.entity.DocumentSegment;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.openai.RequestExecutor;
import com.litevar.agent.openai.completion.CompletionRequestParam;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.DeveloperMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.UserMessage;
import com.litevar.agent.rest.springai.embedding.EmbeddingService;
import com.litevar.agent.rest.util.TikToken;
import com.litevar.agent.rest.vector.MilvusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class FileSummaryService {
    private static final int MAX_TOKENS_PER_CHUNK = 40 * 1024;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是一位专业的信息提炼专家。请阅读以下文章，并根据其标题来引导和组织摘要内容。你的最终输出应是一份百分百忠于原文摘要。
            
            **核心要求**
            1. 标题驱动总结：
            摘要的核心结构和重点必须紧密围绕文章的标题展开。
            首先分析标题所指向的核心议题、问题或对象，然后从文章中提取与该议题最相关的内容进行阐述。
            如果标题是一个问句，摘要应围绕回答该问题来组织；如果标题是一个陈述句，摘要应围绕论证该陈述来组织。
            2. 忠实原文：
            摘要必须严格基于文章内容，不得添加任何个人观点、外部信息或主观臆断。
            客观、准确地转述文章的核心事实、数据和论点。
            3. 长度限制：确保在保留所有关键信息的前提下，用一个段落总结文章的最终结论、核心发现或作者的主要观点。输出内容字数范围必须在2000字内，语言精炼。
            4. 语言风格：
            - 采用客观、中立、专业的书面语。
            - 行文流畅，逻辑严谨，避免口语化表达。
            5. 输出格式要求：你的回复应该只包含上述要求的摘要，不要包含任何前言、说明或总结性语句。
            /noThink
            """;

    @Autowired
    private ModelService modelService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private MilvusService milvusService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;
    @Autowired
    private DocumentService documentService;

    public void summarizeDocument(String datasetId, DatasetDocument document) {
        Dataset dataset = datasetService.getById(datasetId);
        if (StrUtil.isBlank(dataset.getLlmModelId()) || !document.getEnableFlag() || !document.getNeedSummary()) {
            return;
        }
        //1.将该文档下所有片段合并在一起
        String content = segmentService.lambdaQuery()
                .projectDisplay(DocumentSegment::getContent)
                .eq(DocumentSegment::getDocumentId, document.getId())
                .eq(DocumentSegment::getEnableFlag, true)
                .list().stream()
                .map(DocumentSegment::getContent).collect(Collectors.joining("\n\n"));

        //2.生成摘要,如果文档内容少于1000字,直接拿该文档内容作为摘要
        String summarize = content.length() > 1000
                ? summarize(resolveModel(dataset.getLlmModelId()), document.getName(), content)
                : content;

        //3.embedding摘要内容
        Embedding embedding = embeddingService.embedText(summarize, dataset.getEmbeddingModel());

        //4.insert embedding data
        milvusService.insertSummaryEmbedding(
                dataset.getSummaryCollectionName(),
                embedding,
                summarize,
                document.getId()
        );

        document.setNeedSummary(Boolean.FALSE);
        documentService.updateById(document);
    }

    private String summarize(LlmModel model, String title, String content) {
        int tokenCount = TikToken.countTokens(content);
        List<String> chunks;
        if (tokenCount > MAX_TOKENS_PER_CHUNK) {
            List<String> split = TikToken.splitByTokens(content, MAX_TOKENS_PER_CHUNK);
            chunks = IntStream.range(0, split.size()).mapToObj(index ->
                    StrUtil.format("标题:{}\n以下为原文内容的第{}/{}部分,请基于该部分生成符合标题导向的摘要:\n\n{}",
                            title, index + 1, split.size(), split.get(index))
            ).toList();
        } else {
            String message = StrUtil.format("标题: {}\n\n以下为完整正文，请生成符合标题导向的摘要：\n\n{}",
                    title, content);
            chunks = Collections.singletonList(message);
        }

        String summary = "";
        List<String> chunkResultList;
        if (chunks.size() == 1) {
            chunkResultList = List.of(invokeToSummarize(model, chunks.get(0)));
        } else {
            String[] chunkResults = new String[chunks.size()];
            // 并发调用模型生成每个分片的摘要，同时通过索引写回数组以保持顺序
            List<CompletableFuture<Void>> futures = IntStream.range(0, chunks.size())
                    .mapToObj(index -> CompletableFuture.runAsync(() ->
                            chunkResults[index] = invokeToSummarize(model, chunks.get(index)), asyncTaskExecutor))
                    .toList();
            futures.forEach(future -> {
                try {
                    future.join();
                } catch (CompletionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new RuntimeException(cause);
                }
            });
            chunkResultList = List.of(chunkResults);
        }
        //多片的摘要合并为一个摘要
        if (chunkResultList.size() > 1) {
            String aggregated = StrUtil.join("\n\n", chunkResultList);
            String message = StrUtil.format("标题:{},\n\n以下为各分片的摘要,请在严格遵循系统指令的前提下整合为最终摘要:\n\n{}",
                    title, aggregated);
            summary = invokeToSummarize(model, message);
        } else {
            summary = chunkResultList.get(0);
        }

        return summary;
    }

    private String invokeToSummarize(LlmModel model, String content) {
        List<Message> messages = List.of(DeveloperMessage.of(SUMMARY_SYSTEM_PROMPT), UserMessage.of(content));

        CompletionRequestParam param = new CompletionRequestParam();
        param.setMessages(messages);
        param.setModel(model.getName());

        CompletionResponse response = RequestExecutor.doRequest(param, model.getBaseUrl(), model.getApiKey());

        log.info("Generated summary:{}", response);

        return response.getChoices().get(0).getMessage().getContent().trim();
    }

    private LlmModel resolveModel(String llmModelId) {
        return Optional.ofNullable(modelService.findByIdNullable(llmModelId))
                .orElseThrow(() -> new ServiceException("摘要失败!模型不存在,请稍后选择正确的模型"));
    }
}
