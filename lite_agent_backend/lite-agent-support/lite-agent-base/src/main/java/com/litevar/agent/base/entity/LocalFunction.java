package com.litevar.agent.base.entity;

import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author uncle
 * @since 2024/11/15 16:35
 */
@Data
@CollectionName("local_function")
public class LocalFunction extends ToolFunction {
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
