package com.litevar.agent.rest.vector;

import cn.hutool.core.lang.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.*;
import io.milvus.v2.service.vector.request.*;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * @author reid
 * @since 4/27/25
 */

@Service
public class MilvusService {
	private static final String DEFAULT_ID_FIELD_NAME = "id";
	private static final String DEFAULT_TEXT_FIELD_NAME = "text";
	private static final String DEFAULT_METADATA_FIELD_NAME = "metadata";
	private static final String DEFAULT_VECTOR_FIELD_NAME = "vector";

	private static final Logger log = LoggerFactory.getLogger(MilvusService.class);
	private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(LONG_OR_DOUBLE).create();

	@Autowired
	private MilvusClientV2 milvusClient;

	public String insertEmbedding(String collectionName, Embedding embedding, Document segment) {
		List<String> ids = insertEmbeddings(collectionName, singletonList(embedding), singletonList(segment));
		return ids.get(0);
	}

	public List<String> insertEmbeddings(String collectionName, List<Embedding> embeddings, List<Document> segments) {
		List<String> ids = Stream.generate(() -> UUID.randomUUID().toString()).limit(embeddings.size()).toList();

		List<JsonObject> data = new ArrayList<>();
		for (int i = 0; i < ids.size(); i++) {
			JsonObject obj = new JsonObject();
			obj.addProperty(DEFAULT_ID_FIELD_NAME, ids.get(i));
			obj.addProperty(DEFAULT_TEXT_FIELD_NAME, segments.get(i).getText());
			obj.add(DEFAULT_METADATA_FIELD_NAME, GSON.toJsonTree(segments.get(i).getMetadata()).getAsJsonObject());
			obj.add(DEFAULT_VECTOR_FIELD_NAME, GSON.toJsonTree(embeddings.get(i).getOutput()));
			obj.addProperty("enable_flag", true);
			data.add(obj);
		}
		return insertEmbeddings(collectionName, embeddings.get(0).getOutput().length, data);
	}

	private List<String> insertEmbeddings(String collectionName, int dimension, List<JsonObject> data) {
		ensureCollection(collectionName, dimension);

		InsertReq insertReq = InsertReq.builder()
				.collectionName(collectionName)
				.data(data)
				.build();

		InsertResp insertResp = milvusClient.insert(insertReq);
		return insertResp.getPrimaryKeys().stream().map(Object::toString).toList();
	}

	/**
	 * 文档摘要保存embedding数据
	 */
	public void insertSummaryEmbedding(String collectionName, Embedding embedding, String summarize, String documentId) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("documentId", documentId);

		JsonObject obj = new JsonObject();
		obj.addProperty(DEFAULT_ID_FIELD_NAME, documentId);
		obj.addProperty(DEFAULT_TEXT_FIELD_NAME, summarize);
		obj.add(DEFAULT_METADATA_FIELD_NAME, GSON.toJsonTree(metadata).getAsJsonObject());
		obj.add(DEFAULT_VECTOR_FIELD_NAME, GSON.toJsonTree(embedding.getOutput()));
		obj.addProperty("enable_flag", true);

		int dimension = embedding.getOutput().length;
		ensureCollection(collectionName, dimension);

		GetReq getReq = GetReq.builder()
				.collectionName(collectionName)
				.ids(List.of(documentId))
				.build();
		GetResp getResp = milvusClient.get(getReq);
		if (getResp != null && !getResp.getGetResults().isEmpty()) {
			//update
			UpsertReq upsertReq = UpsertReq.builder()
					.collectionName(collectionName)
					.data(List.of(obj))
					.build();
			milvusClient.upsert(upsertReq);
			return;
		}

		insertEmbeddings(collectionName, dimension, List.of(obj));
	}

	public Optional<String> getSummaryText(String collectionName, String documentId) {
		if (!hasCollection(collectionName)) {
			return Optional.empty();
		}

		GetReq getReq = GetReq.builder()
				.collectionName(collectionName)
				.ids(List.of(documentId))
				.outputFields(List.of(DEFAULT_ID_FIELD_NAME, DEFAULT_TEXT_FIELD_NAME))
				.build();

		GetResp getResp = milvusClient.get(getReq);
		if (getResp == null || getResp.getResults == null || getResp.getResults.isEmpty()) {
			return Optional.empty();
		}

		return getResp.getResults.stream()
				.map(result -> result.getEntity().get(DEFAULT_TEXT_FIELD_NAME))
				.filter(Objects::nonNull)
				.map(Object::toString)
				.findFirst();
	}

	public void removeEmbedding(String collectionName, String id) {
		removeEmbeddings(collectionName, singletonList(id));
	}

	public void removeEmbeddings(String collectionName, List<String> ids) {
		if (ids.isEmpty()) {
			return;
		}
		if (!hasCollection(collectionName)) {
			return;
		}

		DeleteReq req = DeleteReq.builder()
			.collectionName(collectionName)
			.ids(asList(ids.toArray()))
			.build();

		milvusClient.delete(req);

		try {
			// Use query to check if collection has any remaining data
			if (isCollectionEmpty(collectionName)) {
				log.info("vector collection '{}' is empty after deletions, dropping it.", collectionName);
				dropCollection(collectionName);
			}
		} catch (Exception e) {
			log.error("Failed to check and drop collection '{}' asynchronously after embedding removal.", collectionName, e);
		}
	}

	public Map<String, Double> search(
        String collectionName, Embedding embedding, int topK, double minScore, List<String> documentIds
    ) {
		String filterExpression = "enable_flag == true";

		String documentIdFilter = (documentIds == null
			? Stream.<String>empty()
			: documentIds.stream().filter(Objects::nonNull).filter(id -> !id.isBlank()))
			.map(id -> "\"" + id.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
			.collect(Collectors.joining(", "));

		if (!documentIdFilter.isEmpty()) {
			filterExpression = filterExpression + " && metadata[\"documentId\"] in [" + documentIdFilter + "]";
		}
//        enable_flag == true && metadata["documentId"] in ["1972595493999550465", "1972596909468098561", "1972595308858777602", "1972594769395785729", "1972594165818662913"]

		SearchReq searchReq = SearchReq.builder()
			.collectionName(collectionName)
			.annsField(DEFAULT_VECTOR_FIELD_NAME)
			.data(singletonList(new FloatVec(embedding.getOutput())))
			.topK(topK)
			.filter(filterExpression) // 根据 enable_flag 以及可选的 documentId 过滤
			.outputFields(asList(DEFAULT_ID_FIELD_NAME, DEFAULT_TEXT_FIELD_NAME, DEFAULT_METADATA_FIELD_NAME))
			.metricType(IndexParam.MetricType.COSINE)
			.consistencyLevel(ConsistencyLevel.EVENTUALLY)
			.build();

		SearchResp searchResp = milvusClient.search(searchReq);

		return searchResp.getSearchResults().get(0).stream()
			.filter(searchResult -> searchResult.getScore().doubleValue() >= minScore) // 过滤掉分数低于 minScore 的结果
			.collect(Collectors.toMap(
				searchResult -> searchResult.getId().toString(), // 提取 id 并转换为 String
				searchResult -> (double) searchResult.getScore() // 提取 score 并转换为 Double
			));
	}

	public List<SearchResp.SearchResult> searchSummaryVector(String collectionName, Embedding embedding, int topK, double minScore) {
		SearchReq searchReq = SearchReq.builder()
				.collectionName(collectionName)
				.annsField(DEFAULT_VECTOR_FIELD_NAME)
				.data(singletonList(new FloatVec(embedding.getOutput())))
				.topK(topK)
				.filter("enable_flag == true") // 只搜索 enable_flag 为 true 的向量
				.outputFields(asList(DEFAULT_ID_FIELD_NAME, DEFAULT_TEXT_FIELD_NAME, DEFAULT_METADATA_FIELD_NAME))
				.metricType(IndexParam.MetricType.COSINE)
				.consistencyLevel(ConsistencyLevel.EVENTUALLY)
				.build();

		return milvusClient.search(searchReq).getSearchResults().get(0).stream()
				.filter(result -> result.getScore().doubleValue() >= minScore)
				.toList();
	}

	public void toggleEnableFlag(String collectionName, List ids, boolean flag) {
        if (!hasCollection(collectionName)) {
            return;
        }
		GetReq getReq = GetReq.builder()
			.collectionName(collectionName)
			.ids(ids)
			.build();

		GetResp getResp = milvusClient.get(getReq);
		if (getResp.getResults.isEmpty()) {
			return; // 如果没有找到对应的记录，直接返回
		}

		List<JsonObject> jsonObjects = getResp.getResults.parallelStream()
			.map(v -> {
				Map<String, Object> entity = v.getEntity();
				entity.put("enable_flag", flag);
				return new Gson().fromJson(new Gson().toJson(entity), JsonObject.class);
			})
			.toList();

		UpsertReq upsertReq = UpsertReq.builder()
			.collectionName(collectionName)
			.data(jsonObjects)
			.build();

		milvusClient.upsert(upsertReq);
	}

	boolean hasCollection(String collectionName) {
		return milvusClient.hasCollection(
			HasCollectionReq.builder().collectionName(collectionName).build()
		);
	}

	void createCollection(String collectionName, int dimension) {
		CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();
		schema.addField(
			AddFieldReq.builder()
				.fieldName(DEFAULT_ID_FIELD_NAME)
				.dataType(DataType.VarChar)
				.isPrimaryKey(true)
				.autoID(false)
				.build()
		);

		schema.addField(
			AddFieldReq.builder()
				.fieldName(DEFAULT_TEXT_FIELD_NAME)
				.dataType(DataType.VarChar)
				.build()
		);

		schema.addField(
			AddFieldReq.builder()
				.fieldName(DEFAULT_METADATA_FIELD_NAME)
				.dataType(DataType.JSON)
				.build()
		);

		schema.addField(
			AddFieldReq.builder()
				.fieldName(DEFAULT_VECTOR_FIELD_NAME)
				.dataType(DataType.FloatVector)
				.dimension(dimension)
				.build()
		);

		schema.addField(
			AddFieldReq.builder()
				.fieldName("enable_flag")
				.dataType(DataType.Bool)
				.defaultValue(Boolean.TRUE)
				.build()
		);

		// indexParams
		List<IndexParam> indexParams = asList(
			IndexParam.builder().fieldName(DEFAULT_ID_FIELD_NAME)
				.indexType(IndexParam.IndexType.AUTOINDEX)
				.build(),
			IndexParam.builder().fieldName("enable_flag")
				.indexType(IndexParam.IndexType.AUTOINDEX)
				.build(),
			IndexParam.builder().fieldName(DEFAULT_VECTOR_FIELD_NAME)
				.indexType(IndexParam.IndexType.FLAT)
				.metricType(IndexParam.MetricType.COSINE)
				.build()
		);

		milvusClient.createCollection(
			CreateCollectionReq.builder()
				.collectionName(collectionName)
				.collectionSchema(schema)
				.indexParams(indexParams)
				.build()
		);
	}

	public void dropCollection(String collectionName) {
		milvusClient.dropCollection(
				DropCollectionReq.builder().collectionName(collectionName).build()
		);

		//如果存在摘要的集合,也要删除
		String summaryCollectionName = collectionName.replace("vector_", "summary_");
		if (hasCollection(summaryCollectionName)) {
			milvusClient.dropCollection(
					DropCollectionReq.builder().collectionName(summaryCollectionName).build()
			);
		}
	}

	void loadCollection(String collectionName) {
		milvusClient.loadCollection(
			LoadCollectionReq.builder().collectionName(collectionName).async(true).build()
		);
	}

	private void ensureCollection(String collectionName, int dimension) {
		if (!hasCollection(collectionName)) {
			createCollection(collectionName, dimension);
			loadCollection(collectionName);
		}
	}

	private boolean isCollectionEmpty(String collectionName) {
		try {
			// Use query to check if any records exist
			QueryReq queryReq = QueryReq.builder()
					.collectionName(collectionName)
					.filter("")
					.outputFields(singletonList(DEFAULT_ID_FIELD_NAME))
					.limit(1L)
					.consistencyLevel(ConsistencyLevel.STRONG)
					.build();

			return milvusClient.query(queryReq).getQueryResults().isEmpty();
		} catch (Exception e) {
			log.warn("Failed to query collection '{}' for emptiness check, assuming not empty", collectionName, e);
			return false;
		}
	}

}
