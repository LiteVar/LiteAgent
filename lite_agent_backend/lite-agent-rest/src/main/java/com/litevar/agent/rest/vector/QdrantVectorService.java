package com.litevar.agent.rest.vector;

import com.litevar.agent.core.module.vector.QdrantPointData;
import com.litevar.agent.core.module.vector.QdrantPointId;
import com.litevar.agent.core.module.vector.QdrantService;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScoredPoint;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Qdrant向量业务服务
 *
 * @author uncle
 * @since 2026/04/28 15:56
 */
@Service
public class QdrantVectorService {
    private static final String ENABLE_FLAG = "enable_flag";
    private static final String DOCUMENT_ID = "documentId";
    private static final String TEXT = "text";

    @Resource
    private QdrantService qdrantService;

    public String insertEmbedding(String collectionName, Embedding embedding, Document segment) {
        List<String> ids = insertEmbeddings(collectionName, List.of(embedding), List.of(segment));
        return ids.get(0);
    }

    public List<String> insertEmbeddings(String collectionName, List<Embedding> embeddings, List<Document> segments) {
        if (embeddings == null || embeddings.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>(embeddings.size());
        List<QdrantPointData> points = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            ids.add(id);

            QdrantPointData point = new QdrantPointData();
            point.setId(QdrantPointId.uuid(id));
            point.setVector(toVector(embeddings.get(i)));
            point.setPayload(buildPayload(segments.get(i).getText(), segments.get(i).getMetadata()));
            points.add(point);
        }
        qdrantService.upsert(collectionName, points);
        return ids;
    }

    public void insertSummaryEmbedding(String collectionName, Embedding embedding, String summarize, String documentId) {
        removeSummaryByDocumentIds(collectionName, List.of(documentId));

        QdrantPointData point = new QdrantPointData();
        point.setId(QdrantPointId.uuid(UUID.randomUUID().toString()));
        point.setVector(toVector(embedding));
        point.setPayload(buildPayload(summarize, Map.of(DOCUMENT_ID, documentId)));
        qdrantService.upsert(collectionName, point);
    }

    public Optional<String> getSummaryText(String collectionName, String documentId) {
        if (!qdrantService.hasCollection(collectionName)) {
            return Optional.empty();
        }
        List<RetrievedPoint> points = qdrantService.scroll(collectionName, buildDocumentFilter(List.of(documentId), true), 1);
        if (points.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getPayloadString(points.get(0).getPayloadMap(), TEXT));
    }

    public Map<String, Double> search(String collectionName,
                                      Embedding embedding,
                                      int topK,
                                      double minScore,
                                      List<String> documentIds) {
        if (!qdrantService.hasCollection(collectionName)) {
            return Collections.emptyMap();
        }
        Filter filter = buildDocumentFilter(documentIds, true);
        List<ScoredPoint> points = qdrantService.search(collectionName, toVector(embedding), topK, (float) minScore, filter);
        Map<String, Double> result = new LinkedHashMap<>();
        for (ScoredPoint point : points) {
            result.put(pointIdToString(point.getId()), (double) point.getScore());
        }
        return result;
    }

    public List<String> searchSummaryDocumentIds(String collectionName, Embedding embedding, int topK, double minScore) {
        if (!qdrantService.hasCollection(collectionName)) {
            return Collections.emptyList();
        }
        List<ScoredPoint> points = qdrantService.search(
                collectionName,
                toVector(embedding),
                topK,
                (float) minScore,
                buildEnableFilter(true)
        );
        return points.stream()
                .map(point -> getPayloadString(point.getPayloadMap(), DOCUMENT_ID))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public void removeEmbedding(String collectionName, String id) {
        removeEmbeddings(collectionName, List.of(id));
    }

    public void removeEmbeddings(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty() || !qdrantService.hasCollection(collectionName)) {
            return;
        }
        qdrantService.delete(collectionName, ids.stream().map(QdrantPointId::uuid).toList());
        dropCollectionIfEmpty(collectionName);
    }

    public void removeSummaryByDocumentIds(String collectionName, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty() || !qdrantService.hasCollection(collectionName)) {
            return;
        }
        qdrantService.delete(collectionName, buildDocumentFilter(documentIds, null));
        dropCollectionIfEmpty(collectionName);
    }

    public void toggleEnableFlag(String collectionName, List<String> ids, boolean flag) {
        if (ids == null || ids.isEmpty() || !qdrantService.hasCollection(collectionName)) {
            return;
        }
        qdrantService.setPayload(
                collectionName,
                Map.of(ENABLE_FLAG, flag),
                ids.stream().map(QdrantPointId::uuid).toList()
        );
    }

    public void toggleSummaryEnableFlagByDocumentIds(String collectionName, List<String> documentIds, boolean flag) {
        if (documentIds == null || documentIds.isEmpty() || !qdrantService.hasCollection(collectionName)) {
            return;
        }
        qdrantService.setPayload(collectionName, Map.of(ENABLE_FLAG, flag), buildDocumentFilter(documentIds, null));
    }

    public void dropCollection(String collectionName) {
        qdrantService.dropCollection(collectionName);
        String summaryCollectionName = collectionName.replace("vector_", "summary_");
        if (!summaryCollectionName.equals(collectionName)) {
            qdrantService.dropCollection(summaryCollectionName);
        }
    }

    private Map<String, Object> buildPayload(String text, Map<String, Object> metadata) {
        Map<String, Object> payload = new HashMap<>();
        if (metadata != null) {
            payload.putAll(metadata);
        }
        payload.put(TEXT, text);
        payload.put(ENABLE_FLAG, true);
        return payload;
    }

    private List<Float> toVector(Embedding embedding) {
        float[] values = embedding.getOutput();
        List<Float> vector = new ArrayList<>(values.length);
        for (float value : values) {
            vector.add(value);
        }
        return vector;
    }

    private Filter buildEnableFilter(boolean flag) {
        return Filter.newBuilder().addMust(ConditionFactory.match(ENABLE_FLAG, flag)).build();
    }

    private Filter buildDocumentFilter(List<String> documentIds, Boolean enableFlag) {
        Filter.Builder builder = Filter.newBuilder();
        if (enableFlag != null) {
            builder.addMust(ConditionFactory.match(ENABLE_FLAG, enableFlag));
        }
        if (documentIds != null) {
            List<String> validIds = documentIds.stream().filter(Objects::nonNull).filter(id -> !id.isBlank()).toList();
            if (!validIds.isEmpty()) {
                builder.addMust(ConditionFactory.matchKeywords(DOCUMENT_ID, validIds));
            }
        }
        return builder.build();
    }

    private String getPayloadString(Map<String, JsonWithInt.Value> payload, String fieldName) {
        JsonWithInt.Value value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case INTEGER_VALUE -> String.valueOf(value.getIntegerValue());
            default -> null;
        };
    }

    private String pointIdToString(PointId pointId) {
        return switch (pointId.getPointIdOptionsCase()) {
            case UUID -> pointId.getUuid();
            case NUM -> String.valueOf(pointId.getNum());
            default -> "";
        };
    }

    private void dropCollectionIfEmpty(String collectionName) {
        if (qdrantService.count(collectionName, null) == 0) {
            qdrantService.dropCollection(collectionName);
        }
    }
}
