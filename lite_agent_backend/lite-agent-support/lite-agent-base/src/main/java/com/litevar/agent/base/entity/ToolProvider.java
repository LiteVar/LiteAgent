package com.litevar.agent.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * OpenApi provider entity
 *
 * @author reid
 * @since 2024/7/29
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tool_provider")
public class ToolProvider {
    @Id
    private String id;

    /**
     * 创建人
     */
    private String userId;
    /**
     * 工作空间id
     */
    @Indexed
    private String workspaceId;

    /**
     * 工具名称
     */
    private String name;
    private String icon;
    /**
     * 描述
     */
    private String description;
    /**
     * schema类型:
     * 1-openapi
     * 2-jsonrpc
     * 3-open_modbus
     */
    private Integer schemaType;
    /**
     * 原始schema描述字符串, yml、json
     */
    private String schemaStr;
    /**
     * 调用工具的apiKey
     */
    private String apiKey;
    /**
     * apiKey类型: Bearer、Basic
     */
    private String apiKeyType;
    /**
     * 分享标记
     */
    private Boolean shareFlag;

    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

}
