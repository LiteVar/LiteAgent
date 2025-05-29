package com.litevar.agent.base.entity;

import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地agent信息
 *
 * @author uncle
 * @since 2024/11/14 10:02
 */
@Data
@CollectionName("local_agent")
public class LocalAgent extends Agent {

    /**
     * jwt token中的uuid
     */
    @MongoIndex
    @CollectionLogic(close = true)
    private String uuid;

    /**
     * 文档过期时间
     */
    @MongoIndex(expireAfterSeconds = 0)
    private LocalDateTime expireTime;
}
