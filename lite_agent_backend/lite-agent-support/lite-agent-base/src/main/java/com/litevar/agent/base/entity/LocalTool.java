package com.litevar.agent.base.entity;

import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地tool
 *
 * @author uncle
 * @since 2024/11/15 15:02
 */
@Data
@CollectionName("local_tool")
public class LocalTool extends ToolProvider {
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