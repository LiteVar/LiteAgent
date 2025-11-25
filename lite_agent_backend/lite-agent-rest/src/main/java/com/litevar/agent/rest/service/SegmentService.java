package com.litevar.agent.rest.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.entity.DatasetRetrieveHistory;
import com.litevar.agent.base.entity.DocumentSegment;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.base.vo.SegmentUpdateForm;
import com.litevar.agent.base.vo.SegmentVO;
import com.litevar.agent.rest.springai.embedding.EmbeddingService;
import com.litevar.agent.rest.util.TikToken;
import com.litevar.agent.rest.vector.MilvusService;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Service implementation for Segment management.
 */
@Slf4j
@Service
public class SegmentService extends ServiceImpl<DocumentSegment> {
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private DatasetRetrieveHistoryService retrieveHistoryService;

    @Autowired
    private MilvusService milvusService;

    /**
     * Creates a new segment.
     *
     * @param documentId the ID of the document
     * @param form       the segment to create
     * @return the created segment
     */
    public DocumentSegment createSegment(String documentId, SegmentUpdateForm form) {
        DatasetDocument document = documentService.getById(documentId);
        Dataset dataset = datasetService.getDataset(document.getDatasetId());

        JSONObject metadata = StrUtil.isNotBlank(form.getMetadata()) ? JSONUtil.parseObj(form.getMetadata()) : new JSONObject();
        metadata.putIfAbsent("documentId", documentId);
        metadata.putIfAbsent("datasetId", dataset.getId());

        //embed segment
        Document textSegment = Document.builder().text(form.getContent()).metadata(metadata.toBean(Map.class)).build();
        Embedding embedding = embeddingService.embedText(form.getContent(), dataset.getEmbeddingModel());
        String embeddingId = milvusService.insertEmbedding(dataset.getVectorCollectionName(), embedding, textSegment);

        DocumentSegment segment = BeanUtil.toBean(form, DocumentSegment.class, CopyOptions.create().setIgnoreNullValue(true));
        segment.setUserId(document.getUserId());
        segment.setWorkspaceId(document.getWorkspaceId());
        segment.setDatasetId(document.getDatasetId());
        segment.setDocumentId(documentId);
        segment.setWordCount(form.getContent().length());
        segment.setTokenCount(TikToken.countTokens(form.getContent()));
        segment.setMetadata(metadata.toString());
        segment.setEmbeddingId(embeddingId);
        segment.setVectorCollectionName(dataset.getVectorCollectionName());
        save(segment);

        document.setWordCount(document.getWordCount() + segment.getWordCount());
        document.setTokenCount(document.getTokenCount() + segment.getTokenCount());
        document.setNeedSummary(Boolean.TRUE);
        documentService.updateById(document);

        return segment;
    }

    public List<DocumentSegment> embedSegments(
        String workspaceId, String datasetId, String documentId, String fileId, List<Document> segments
    ) {
        Dataset dataset = datasetService.getDataset(datasetId);
        List<Embedding> embeddings = embeddingService.embedSegments(segments, dataset.getEmbeddingModel());
        List<String> embeddingIds = milvusService.insertEmbeddings(dataset.getVectorCollectionName(), embeddings, segments);

        List<DocumentSegment> documentSegments = new ArrayList<>(segments.size());

        IntStream.range(0, segments.size()).forEach(i -> {
            Document segment = segments.get(i);
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setUserId(dataset.getUserId());
            docSegment.setWorkspaceId(workspaceId);
            docSegment.setDatasetId(datasetId);
            docSegment.setDocumentId(documentId);
            docSegment.setFileId(fileId);
            docSegment.setContent(segment.getText());
            docSegment.setWordCount(segment.getText().length());
            docSegment.setTokenCount(TikToken.countTokens(segment.getText()));
            docSegment.setEmbeddingId(embeddingIds.get(i));
            docSegment.setVectorCollectionName(dataset.getVectorCollectionName());
            docSegment.setHitCount(0);
            docSegment.setEnableFlag(true);
            docSegment.setMetadata(JSONUtil.toJsonStr(segment.getMetadata()));
            documentSegments.add(docSegment);
        });

        saveBatch(documentSegments);
        return documentSegments;
    }

    /**
     * Lists segments in a document with pagination.
     *
     * @param documentId the ID of the document
     * @param pageNo     the page number
     * @param pageSize   the page size
     * @return a page model containing the segments
     */
    public PageModel<DocumentSegment> listSegments(String documentId, String query, Integer pageNo, Integer pageSize) {
        LambdaQueryChainWrapper<DocumentSegment> wrapper = this.lambdaQuery()
                .eq(DocumentSegment::getDocumentId, documentId)
                .like(StrUtil.isNotBlank(query), DocumentSegment::getContent, query)
                .orderByAsc(DocumentSegment::getId);
        PageResult<DocumentSegment> pageResult = this.page(wrapper, pageNo, pageSize);

        return new PageModel<>(pageNo, pageSize, pageResult.getTotalSize(), pageResult.getContentData());
    }

    /**
     * Updates an existing segment.
     *
     * @param id   the ID of the segment to update
     * @param form the new data for the segment
     * @return the updated segment
     */
    public DocumentSegment updateSegment(String id, SegmentUpdateForm form) {
        DocumentSegment segment = getById(id);
        Dataset dataset = datasetService.getDataset(segment.getDatasetId());

        JSONObject metadata = StrUtil.isNotBlank(form.getMetadata()) ? JSONUtil.parseObj(form.getMetadata()) : new JSONObject();
        metadata.putIfAbsent("documentId", segment.getDocumentId());
        metadata.putIfAbsent("datasetId", dataset.getId());
        segment.setMetadata(metadata.toString());

        if (!StrUtil.equals(form.getContent(), segment.getContent())) {

            int oldWordCount = segment.getWordCount();
            int oldTokenCount = segment.getTokenCount();

            // Only update allowed fields
            segment.setContent(form.getContent());
            segment.setWordCount(form.getContent().length());
            segment.setTokenCount(TikToken.countTokens(form.getContent()));
            segment.setVectorCollectionName(dataset.getVectorCollectionName());

            //remove old embedding
            milvusService.removeEmbedding(dataset.getVectorCollectionName(), segment.getEmbeddingId());

            //embed new segment
            Document newSegment = Document.builder().text(segment.getContent()).metadata(metadata.toBean(Map.class)).build();
            Embedding embedding = embeddingService.embedText(segment.getContent(), dataset.getEmbeddingModel());
            String embeddingId = milvusService.insertEmbedding(dataset.getVectorCollectionName(), embedding, newSegment);

            segment.setEmbeddingId(embeddingId);

            DatasetDocument document = documentService.getById(segment.getDocumentId());
            document.setWordCount(document.getWordCount() - oldWordCount + segment.getWordCount());
            document.setTokenCount(document.getTokenCount() - oldTokenCount + segment.getTokenCount());
            document.setNeedSummary(Boolean.TRUE);
            documentService.updateById(document);
        }
        updateById(segment);
        return segment;
    }

    public void deleteSegments(List<String> ids) {
        List<DocumentSegment> segments = getByIds(ids);

        int wordCount = segments.stream().mapToInt(DocumentSegment::getWordCount).sum();
        int tokenCount = segments.stream().mapToInt(DocumentSegment::getTokenCount).sum();

        // update document word/token count
        DatasetDocument document = documentService.getById(segments.get(0).getDocumentId());
        document.setWordCount(document.getWordCount() - wordCount);
        document.setTokenCount(document.getTokenCount() - tokenCount);
        document.setNeedSummary(Boolean.TRUE);
        documentService.updateById(document);

        // remove embeddings
        milvusService.removeEmbeddings(segments.get(0).getVectorCollectionName(), segments.stream().map(DocumentSegment::getEmbeddingId).toList());

        // remove segments
        this.removeBatchByIds(segments.stream().map(DocumentSegment::getId).toList());
    }

    /**
     * Deletes segments by dataset ID.
     *
     * @param datasetId the ID of the dataset
     */
    public void deleteSegmentsByDatasetId(String datasetId) {
        LambdaQueryChainWrapper<DocumentSegment> wrapper = this.lambdaQuery()
            .eq(DocumentSegment::getDatasetId, datasetId);
        List<DocumentSegment> segments = wrapper.list();

        if (segments.isEmpty()) {
            return;
        }

        // drop collection
        milvusService.dropCollection(segments.get(0).getVectorCollectionName());
        // remove segments
        this.removeBatchByIds(segments.stream().map(DocumentSegment::getId).toList());
    }

    public void deleteSegmentsByDocumentIds(List<String> documentIds) {
        LambdaQueryChainWrapper<DocumentSegment> wrapper = this.lambdaQuery()
            .in(DocumentSegment::getDocumentId, documentIds);
        List<DocumentSegment> segments = wrapper.list();

        if (segments.isEmpty()) {
            return;
        }

        // remove embeddings
        milvusService.removeEmbeddings(
            segments.get(0).getVectorCollectionName(),
            segments.stream().map(DocumentSegment::getEmbeddingId).toList()
        );
        // remove segments
        this.removeBatchByIds(segments.stream().map(DocumentSegment::getId).toList());
    }

    /**
     * Toggles the enable flag of a segment.
     */
    public void toggleEnableFlag(List<DocumentSegment> segmentList, boolean flag) {
        if (segmentList.isEmpty()) {
            return;
        }
        segmentList.parallelStream().forEach(segment -> segment.setEnableFlag(flag));

        updateBatchByIds(segmentList);

        List<String> embeddingIds = segmentList.stream().map(DocumentSegment::getEmbeddingId).toList();

        milvusService.toggleEnableFlag(segmentList.get(0).getVectorCollectionName(), embeddingIds, flag);
    }

    /**
     * retrieves segments base on similarity
     */
    public List<SegmentVO> retrieveSegments(
        String agentId, List<String> datasetIds, String content
    ) {
        return (List<SegmentVO>) retrieve(agentId, datasetIds, content).get("result");
    }

    public Dict retrieve(String agentId, List<String> datasetIds, String content) {
        List<Dataset> datasetList = datasetService.getByIds(datasetIds);
        List<String> dsIds = datasetList.parallelStream().map(Dataset::getId).toList();
        if (dsIds.isEmpty()) {
            return Dict.create().set("result", Collections.emptyList());
        }

        List<OutMessage.KnowledgeHistoryInfo> historyInfoList = new ArrayList<>();
        List<DatasetRetrieveHistory> historyList = new ArrayList<>();
        List<SegmentVO> segmentVOS = new ArrayList<>();

        for (Dataset dataset : datasetList) {
            Embedding embedding = embeddingService.embedText(content, dataset.getEmbeddingModel());

            DatasetRetrieveHistory history = retrieveHistoryService.createHistory(dataset.getId(), agentId, content);
            historyList.add(history);

            List<String> summaryDocId = new ArrayList<>();
            if (StrUtil.isNotBlank(dataset.getSummaryCollectionName())) {
                //先查询摘要,过滤文档
                List<SearchResp.SearchResult> summaryResult = milvusService.searchSummaryVector(
                        dataset.getSummaryCollectionName(), embedding, dataset.getRetrievalTopK(), dataset.getRetrievalScoreThreshold());
                if (!summaryResult.isEmpty()) {
                    log.info("搜索{}摘要集合,命中片段数量:{}", dataset.getSummaryCollectionName(), summaryResult.size());
                    //提取documentId
                    summaryResult.forEach(i -> summaryDocId.add(i.getId().toString()));
                } else {
                    log.warn("搜索{}摘要集合,未命中片段,将不再继续搜索该知识库", dataset.getSummaryCollectionName());
                    continue;
                }
            }

            Map<String, Double> result = milvusService.search(
                    dataset.getVectorCollectionName(),
                    embedding,
                    dataset.getRetrievalTopK(),
                    dataset.getRetrievalScoreThreshold(),
                    summaryDocId);
            if (!result.isEmpty()) {
                OutMessage.KnowledgeHistoryInfo info = new OutMessage.KnowledgeHistoryInfo();
                info.setId(history.getId());
                info.setDatasetName(dataset.getName());
                info.setDatasetId(dataset.getId());
                historyInfoList.add(info);

                List<DocumentSegment> segmentList = this.lambdaQuery()
                        .eq(DocumentSegment::getDatasetId, dataset.getId())
                        .in(DocumentSegment::getEmbeddingId, result.keySet())
                        .eq(DocumentSegment::getEnableFlag, true).list();
                //update hit count
                segmentList.parallelStream().forEach(segment -> segment.setHitCount(segment.getHitCount() + 1));
                this.updateBatchByIds(segmentList);

                List<SegmentVO> voList = segmentList.parallelStream().map(segment -> {
                    SegmentVO vo = BeanUtil.copyProperties(segment, SegmentVO.class);
                    vo.setScore(result.get(segment.getEmbeddingId()));
                    vo.setDocumentName(documentService.getDocument(segment.getDocumentId()).getName());
                    return vo;
                }).sorted(Comparator.comparing(SegmentVO::getScore).reversed()).toList();
                segmentVOS.addAll(voList);

                List<DatasetRetrieveHistory.RetrieveSegment> retrieveSegmentList = BeanUtil.copyToList(voList, DatasetRetrieveHistory.RetrieveSegment.class);
                history.setRetrieveSegmentList(retrieveSegmentList);
            }
        }
        if (!historyList.isEmpty()) {
            retrieveHistoryService.saveBatch(historyList);
        }
        return Dict.create().set("result", segmentVOS).set("history", historyInfoList);
    }

}
