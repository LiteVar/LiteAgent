package com.litevar.agent.base.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 工作空间
 *
 * @author uncle
 * @since 2024/8/1 11:12
 */
@Data
@Document("workspace")
public class Workspace {

    private String id;
    /**
     * 工作空间名字
     */
    @Indexed(unique = true)
    private String name;

    @CreatedDate
    private LocalDateTime createTime;
}
