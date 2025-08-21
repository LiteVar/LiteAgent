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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		ensureCollection(collectionName, embeddings.get(0).getOutput().length);

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

		InsertReq insertReq = InsertReq.builder()
				.collectionName(collectionName)
				.data(data)
				.build();

		InsertResp insertResp = milvusClient.insert(insertReq);
		return insertResp.getPrimaryKeys().stream()
				.map(Object::toString) // 提取 id 并转换为 String
				.toList();
	}

	public void removeEmbedding(String collectionName, String id) {
		removeEmbeddings(collectionName, singletonList(id));
	}

	public void removeEmbeddings(String collectionName, List<String> ids) {
		if (ids.isEmpty()) {
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

	public Map<String, Double> search(String collectionName, Embedding embedding, int topK, double minScore) {
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

		SearchResp searchResp = milvusClient.search(searchReq);

		return searchResp.getSearchResults().get(0).stream()
			.filter(searchResult -> searchResult.getScore().doubleValue() >= minScore) // 过滤掉分数低于 minScore 的结果
			.collect(Collectors.toMap(
				searchResult -> searchResult.getId().toString(), // 提取 id 并转换为 String
				searchResult -> (double) searchResult.getScore() // 提取 score 并转换为 Double
			));
	}

	public void toggleEnableFlag(String collectionName, List ids, boolean flag) {
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

		// 强制清理VectorService中的collection schema缓存
		// 避免重新创建同名但不同dimension的collection时使用旧缓存
		try {
			java.lang.reflect.Field vectorServiceField = milvusClient.getClass().getDeclaredField("vectorService");
			vectorServiceField.setAccessible(true);
			Object vectorService = vectorServiceField.get(milvusClient);

			java.lang.reflect.Field cacheField = vectorService.getClass().getDeclaredField("cacheCollectionInfo");
			cacheField.setAccessible(true);
			Object cacheObject = cacheField.get(vectorService);
			if (cacheObject instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cache = (Map<String, Object>) cacheObject;
				cache.remove(collectionName);
				log.debug("Cleared collection schema cache for '{}'", collectionName);
			}
		} catch (Exception e) {
			log.warn("Failed to clear collection schema cache for '{}', may cause issues when recreating collection with different schema", collectionName, e);
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
