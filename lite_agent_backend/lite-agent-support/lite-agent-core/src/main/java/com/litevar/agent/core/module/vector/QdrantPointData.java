package com.litevar.agent.core.module.vector;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Qdrant点位数据
 *
 * @author uncle
 * @since 2026/04/28 10:09
 */
@Data
public class QdrantPointData {
    /**
     * pointId
     */
    private QdrantPointId id;
    /**
     * 向量值
     */
    private List<Float> vector;
    /**
     * 载荷数据
     */
    private Map<String, Object> payload;
}
