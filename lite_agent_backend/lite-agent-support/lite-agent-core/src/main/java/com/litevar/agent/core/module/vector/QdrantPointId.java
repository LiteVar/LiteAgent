package com.litevar.agent.core.module.vector;

import io.qdrant.client.PointIdFactory;
import io.qdrant.client.grpc.Common.PointId;
import lombok.Data;

import java.util.UUID;

/**
 * Qdrant点位主键
 *
 * @author uncle
 * @since 2026/04/28 10:09
 */
@Data
public class QdrantPointId {
    private final String uuid;
    private final Long number;

    private QdrantPointId(String uuid, Long number) {
        this.uuid = uuid;
        this.number = number;
    }

    public static QdrantPointId uuid(String uuid) {
        return new QdrantPointId(uuid, null);
    }

    public static QdrantPointId number(long number) {
        return new QdrantPointId(null, number);
    }

    public PointId toPointId() {
        if (uuid != null) {
            return PointIdFactory.id(UUID.fromString(uuid));
        }
        return PointIdFactory.id(number);
    }
}
