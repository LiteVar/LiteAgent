package com.litevar.agent.core.module.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.grpc.Collections.*;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.*;
import io.qdrant.client.grpc.Points.Vector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Qdrant基础操作服务
 *
 * @author uncle
 * @since 2026/04/28 10:09
 */
@Service
@ConditionalOnBean(QdrantClient.class)
public class QdrantService {
    private static final String ENABLE_FLAG = "enable_flag";
    private static final String DEFAULT_VECTOR_NAME = "vector";

    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper;

    public QdrantService(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
        this.objectMapper = new ObjectMapper();
    }

    public void upsert(String collectionName, QdrantPointData point) {
        upsert(collectionName, Collections.singletonList(point));
    }

    public void upsert(String collectionName, List<QdrantPointData> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        ensureCollection(collectionName, points.get(0).getVector().size());
        try {
            await(qdrantClient.upsertAsync(collectionName, toPointStructs(points)));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "写入Qdrant点位失败:" + e.getMessage());
        }
    }

    public List<RetrievedPoint> retrieve(String collectionName, List<QdrantPointId> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return await(qdrantClient.retrieveAsync(
                    collectionName,
                    toPointIds(ids),
                    true,
                    false,
                    null
            ));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "查询Qdrant点位失败:" + e.getMessage());
        }
    }

    public List<ScoredPoint> search(String collectionName,
                                    List<Float> vector,
                                    int limit,
                                    Float scoreThreshold,
                                    Filter filter) {
        SearchPoints.Builder builder = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(vector)
                .setVectorName(DEFAULT_VECTOR_NAME)
                .setLimit(limit)
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .setWithVectors(WithVectorsSelectorFactory.enable(false));
        if (filter != null) {
            builder.setFilter(filter);
        }
        if (scoreThreshold != null) {
            builder.setScoreThreshold(scoreThreshold);
        }
        try {
            return await(qdrantClient.searchAsync(builder.build()));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "搜索Qdrant向量失败:" + e.getMessage());
        }
    }

    public void setPayload(String collectionName, Map<String, Object> payload, List<QdrantPointId> ids) {
        if (ids == null || ids.isEmpty() || payload == null || payload.isEmpty()) {
            return;
        }
        try {
            await(qdrantClient.setPayloadAsync(
                    collectionName,
                    toPayload(payload),
                    toPointIds(ids),
                    true,
                    null,
                    null
            ));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "更新Qdrant payload失败:" + e.getMessage());
        }
    }

    public void setPayload(String collectionName, Map<String, Object> payload, Filter filter) {
        if (filter == null || payload == null || payload.isEmpty()) {
            return;
        }
        try {
            await(qdrantClient.setPayloadAsync(
                    collectionName,
                    toPayload(payload),
                    filter,
                    true,
                    null,
                    null
            ));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "按条件更新Qdrant payload失败:" + e.getMessage());
        }
    }

    public void delete(String collectionName, List<QdrantPointId> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try {
            await(qdrantClient.deleteAsync(collectionName, toPointIds(ids)));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "删除Qdrant点位失败:" + e.getMessage());
        }
    }

    public void delete(String collectionName, Filter filter) {
        if (filter == null) {
            return;
        }
        try {
            await(qdrantClient.deleteAsync(collectionName, filter));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "按条件删除Qdrant点位失败:" + e.getMessage());
        }
    }

    public List<RetrievedPoint> scroll(String collectionName, Filter filter, int limit) {
        ScrollPoints.Builder builder = ScrollPoints.newBuilder()
                .setCollectionName(collectionName)
                .setLimit(limit)
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .setWithVectors(WithVectorsSelectorFactory.enable(false));
        if (filter != null) {
            builder.setFilter(filter);
        }
        try {
            return await(qdrantClient.scrollAsync(builder.build())).getResultList();
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "滚动查询Qdrant点位失败:" + e.getMessage());
        }
    }

    public long count(String collectionName, Filter filter) {
        try {
            return await(qdrantClient.countAsync(collectionName, filter, true, null));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "统计Qdrant点位数量失败:" + e.getMessage());
        }
    }

    public boolean hasCollection(String collectionName) {
        try {
            return await(qdrantClient.collectionExistsAsync(collectionName));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "检查Qdrant集合失败:" + e.getMessage());
        }
    }

    private void ensureCollection(String collectionName, int dimension) {
        if (hasCollection(collectionName)) {
            return;
        }
        CreateCollection createCollection = CreateCollection.newBuilder()
                .setCollectionName(collectionName)
                .setVectorsConfig(VectorsConfig.newBuilder()
                        .setParamsMap(VectorParamsMap.newBuilder()
                                .putMap(DEFAULT_VECTOR_NAME, VectorParams.newBuilder()
                                        .setSize(dimension)
                                        .setDistance(Distance.Cosine)
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            await(qdrantClient.createCollectionAsync(createCollection));
            await(qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    ENABLE_FLAG,
                    PayloadSchemaType.Bool,
                    PayloadIndexParams.newBuilder().build(),
                    true,
                    null,
                    null
            ));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "创建Qdrant集合失败:" + e.getMessage());
        }
    }

    public void dropCollection(String collectionName) {
        if (!hasCollection(collectionName)) {
            return;
        }
        try {
            await(qdrantClient.deleteCollectionAsync(collectionName));
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "删除Qdrant集合失败:" + e.getMessage());
        }
    }

    private List<PointStruct> toPointStructs(List<QdrantPointData> points) {
        List<PointStruct> pointStructs = new ArrayList<>(points.size());
        for (QdrantPointData point : points) {
            pointStructs.add(PointStruct.newBuilder()
                    .setId(point.getId().toPointId())
                    .setVectors(Vectors.newBuilder()
                            .setVectors(NamedVectors.newBuilder()
                                    .putVectors(DEFAULT_VECTOR_NAME, Vector.newBuilder().addAllData(point.getVector()).build())
                                    .build())
                            .build())
                    .putAllPayload(toPayload(point.getPayload()))
                    .build());
        }
        return pointStructs;
    }

    private List<PointId> toPointIds(List<QdrantPointId> ids) {
        return ids.stream().filter(Objects::nonNull).map(QdrantPointId::toPointId).toList();
    }

    private Map<String, JsonWithInt.Value> toPayload(Map<String, Object> payload) {
        JsonNode jsonNode = objectMapper.valueToTree(payload);
        JsonWithInt.Struct.Builder builder = JsonWithInt.Struct.newBuilder();
        jsonNode.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isNull()) {
                builder.putFields(entry.getKey(), toValue(entry.getValue()));
            }
        });
        return builder.getFieldsMap();
    }

    private JsonWithInt.Value toValue(JsonNode node) {
        if (node.isTextual()) {
            return ValueFactory.value(node.textValue());
        }
        if (node.isBoolean()) {
            return ValueFactory.value(node.booleanValue());
        }
        if (node.isIntegralNumber()) {
            return ValueFactory.value(node.longValue());
        }
        if (node.isFloatingPointNumber()) {
            return ValueFactory.value(node.doubleValue());
        }
        if (node.isArray()) {
            List<JsonWithInt.Value> values = new ArrayList<>(node.size());
            node.forEach(child -> values.add(toValue(child)));
            return JsonWithInt.Value.newBuilder()
                    .setListValue(JsonWithInt.ListValue.newBuilder().addAllValues(values).build())
                    .build();
        }
        if (node.isObject()) {
            JsonWithInt.Struct.Builder structBuilder = JsonWithInt.Struct.newBuilder();
            node.fields().forEachRemaining(entry -> {
                if (!entry.getValue().isNull()) {
                    structBuilder.putFields(entry.getKey(), toValue(entry.getValue()));
                }
            });
            return JsonWithInt.Value.newBuilder().setStructValue(structBuilder.build()).build();
        }
        return ValueFactory.value(node.asText());
    }

    private <T> T await(ListenableFuture<T> future) throws ExecutionException, InterruptedException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
