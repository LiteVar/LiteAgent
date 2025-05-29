package com.litevar.agent.base.entity;

import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地model
 *
 * @author uncle
 * @since 2024/11/15 14:29
 */
@Data
@CollectionName("local_model")
public class LocalModel extends LlmModel {
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