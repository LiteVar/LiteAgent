package com.litevar.agent.base.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 本地tool
 *
 * @author uncle
 * @since 2024/11/15 15:02
 */
@Data
@Document(collection = "local_tool")
public class LocalTool extends ToolProvider {
    /**
     * jwt token中的uuid
     */
    @Indexed
    private String uuid;
    /**
     * 文档过期时间
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expireTime;
}