package com.litevar.agent.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * agents 数据表
 *
 * @author reid
 * @since 2024/8/8
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent")
public class Agent {
    @Id
    private String id;

    /**
     * 创建者id
     */
    private String userId;
    /**
     * 工作空间id
     */
    private String workspaceId;

    /**
     * agent名称
     */
    private String name;
    /**
     * 图标url
     */
    private String icon = "";
    /**
     * 描述,可空
     */
    private String description = "";
    /**
     * 提示词
     */
    private String prompt = "";
    /**
     * 关联的大模型id
     */
    private String llmModelId = "";
    /**
     * 关联的工具列表
     */
    private List<String> toolIds = Collections.emptyList();
    /**
     * agent状态: 0-初始待发布,1-已发布生效
     */
    private Integer status = 0;
    /**
     * 是否开启分享
     */
    private Boolean shareFlag = Boolean.FALSE;

    /**
     * 温度值
     */
    private Double temperature;

    /**
     * 概率抽样的 p 值
     */
    private Double topP;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;

    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

}
