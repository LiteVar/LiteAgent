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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
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
		Embedding embedding = embeddingService.embedSegment(textSegment, dataset.getLlmModelId());
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
		documentService.updateById(document);

		return segment;
	}

	public List<DocumentSegment> embedSegments(
		String workspaceId, String datasetId, String documentId, List<Document> segments
	) {
		Dataset dataset = datasetService.getDataset(datasetId);
		List<Embedding> embeddings = embeddingService.embedSegments(segments, dataset.getLlmModelId());
		List<String> embeddingIds = milvusService.insertEmbeddings(dataset.getVectorCollectionName(), embeddings, segments);

		List<DocumentSegment> documentSegments = new ArrayList<>(segments.size());

		IntStream.range(0, segments.size()).forEach(i -> {
			Document segment = segments.get(i);
			DocumentSegment docSegment = new DocumentSegment();
			docSegment.setUserId(dataset.getUserId());
			docSegment.setWorkspaceId(workspaceId);
			docSegment.setDatasetId(datasetId);
			docSegment.setDocumentId(documentId);
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
			.eq(DocumentSegment::getDocumentId, documentId);

		if (StrUtil.isNotBlank(query)) {
			wrapper.like(DocumentSegment::getContent, query);
		}

		wrapper.orderByAsc(DocumentSegment::getId);
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

		int oldWordCount = segment.getWordCount();
		int oldTokenCount = segment.getTokenCount();

		// Only update allowed fields
		segment.setContent(form.getContent());
		segment.setWordCount(form.getContent().length());
		segment.setTokenCount(TikToken.countTokens(form.getContent()));
//        segment.setMetadata(form.getMetadata());
		segment.setVectorCollectionName(dataset.getVectorCollectionName());

		JSONObject metadata = StrUtil.isNotBlank(form.getMetadata()) ? JSONUtil.parseObj(form.getMetadata()) : new JSONObject();
		metadata.putIfAbsent("documentId", segment.getDocumentId());
		metadata.putIfAbsent("datasetId", dataset.getId());
		segment.setMetadata(metadata.toString());

		//remove old embedding
		milvusService.removeEmbedding(dataset.getVectorCollectionName(), segment.getEmbeddingId());

		//embed new segment
		Document newSegment = Document.builder().text(segment.getContent()).metadata(metadata.toBean(Map.class)).build();
		Embedding embedding = embeddingService.embedSegment(newSegment, dataset.getLlmModelId());
		String embeddingId = milvusService.insertEmbedding(dataset.getVectorCollectionName(), embedding, newSegment);

		segment.setEmbeddingId(embeddingId);
		updateById(segment);

		DatasetDocument document = documentService.getById(segment.getDocumentId());
		document.setWordCount(document.getWordCount() - oldWordCount + segment.getWordCount());
		document.setTokenCount(document.getTokenCount() - oldTokenCount + segment.getTokenCount());
		documentService.updateById(document);

		return segment;
	}

	/**
	 * Deletes a segment by its ID.
	 *
	 * @param id the ID of the segment to delete
	 */
	public void deleteSegment(String id) {
		DocumentSegment segment = getById(id);

		DatasetDocument document = documentService.getById(segment.getDocumentId());
		document.setWordCount(document.getWordCount() - segment.getWordCount());
		document.setTokenCount(document.getTokenCount() - segment.getTokenCount());
		documentService.updateById(document);

		// remove embeddings
		milvusService.removeEmbedding(segment.getVectorCollectionName(), segment.getEmbeddingId());

		removeById(id);
	}

	public void deleteSegments(List<String> ids) {
		List<DocumentSegment> segments = getByIds(ids);

		int wordCount = segments.stream().mapToInt(DocumentSegment::getWordCount).sum();
		int tokenCount = segments.stream().mapToInt(DocumentSegment::getTokenCount).sum();

		// update document word/token count
		DatasetDocument document = documentService.getById(segments.get(0).getDocumentId());
		document.setWordCount(document.getWordCount() - wordCount);
		document.setTokenCount(document.getTokenCount() - tokenCount);
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

	/**
	 * Deletes segments by document ID.
	 *
	 * @param documentId the ID of the document
	 */
	public void deleteSegmentsByDocumentId(String documentId) {
		LambdaQueryChainWrapper<DocumentSegment> wrapper = this.lambdaQuery()
			.eq(DocumentSegment::getDocumentId, documentId);
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
	 *
	 * @param id the ID of the segment
	 */
	public void toggleEnableFlag(String id) {
		DocumentSegment segment = getById(id);

		boolean flag = !segment.getEnableFlag();
		segment.setEnableFlag(flag);
		saveOrUpdate(segment);

		milvusService.toggleEnableFlag(segment.getVectorCollectionName(), List.of(segment.getEmbeddingId()), flag);
	}

	/**
	 * Toggles the enable flag of all segments in a document.
	 *
	 * @param documentId the ID of the document
	 * @param flag       the new flag value
	 */
	public void toggleEnableFlagByDocument(String documentId, boolean flag) {
		List<DocumentSegment> segments = this.getByColumn("document_id", documentId);
		segments.parallelStream().forEach(segment -> segment.setEnableFlag(flag));
		saveOrUpdateBatch(segments);

		List<String> embeddingIds = segments.stream().map(DocumentSegment::getEmbeddingId).toList();
		milvusService.toggleEnableFlag(segments.get(0).getVectorCollectionName(), embeddingIds, flag);
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
		List<DatasetRetrieveHistory> history = retrieveHistoryService.createHistory(dsIds, agentId, content, StrUtil.isBlank(agentId) ? "TEST" : "AGENT");
		Map<String, String> datasetMap = datasetList.parallelStream().collect(Collectors.toMap(Dataset::getId, Dataset::getName));
		List<OutMessage.KnowledgeHistoryInfo> dh = history.stream().map(i -> {
			String name = datasetMap.get(i.getDatasetId());
			//历史记录id,数据集名称
			OutMessage.KnowledgeHistoryInfo info = new OutMessage.KnowledgeHistoryInfo();
			info.setId(i.getId());
			info.setDatasetName(name);
			info.setDatasetId(i.getDatasetId());
			return info;
		}).toList();

		Map<String, Double> idScoreMap = datasetList.parallelStream().map(
				dataset -> {
					Embedding embedding = embeddingService.embedText(content, dataset.getLlmModelId());
					return milvusService.search(dataset.getVectorCollectionName(), embedding, dataset.getRetrievalTopK(), dataset.getRetrievalScoreThreshold());
				}
			)
			.flatMap(map -> map.entrySet().stream())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		List<SegmentVO> segmentVOs = new ArrayList<>();
		if (!idScoreMap.isEmpty()) {
			List<DocumentSegment> segments = this.lambdaQuery()
				.in(DocumentSegment::getEmbeddingId, idScoreMap.keySet())
				.in(DocumentSegment::getDatasetId, datasetIds)
				.eq(DocumentSegment::getEnableFlag, true)
				.list();

			if (!segments.isEmpty()) {
				//update hit count
				segments.parallelStream().forEach(segment -> segment.setHitCount(segment.getHitCount() + 1));
				saveOrUpdateBatch(segments);

				List<SegmentVO> list = segments.parallelStream().map(segment -> {
					SegmentVO vo = BeanUtil.toBean(segment, SegmentVO.class);
					vo.setScore(idScoreMap.get(segment.getEmbeddingId()));
					vo.setDocumentName(documentService.getDocument(segment.getDocumentId()).getName());
					return vo;
				}).sorted(Comparator.comparing(SegmentVO::getScore).reversed()).toList();
				segmentVOs.addAll(list);

				Map<String, List<SegmentVO>> datasetSegment = list.parallelStream().collect(Collectors.groupingBy(SegmentVO::getDatasetId));
				history.forEach(h -> {
					List<SegmentVO> vo = datasetSegment.get(h.getDatasetId());
					if (vo != null) {
						List<DatasetRetrieveHistory.RetrieveSegment> retrieveSegments =
							BeanUtil.copyToList(vo, DatasetRetrieveHistory.RetrieveSegment.class);

						h.setRetrieveSegmentList(retrieveSegments);
					}
				});
			}
		}

		retrieveHistoryService.saveBatch(history);
		return Dict.create().set("result", segmentVOs).set("history", dh);
	}

}