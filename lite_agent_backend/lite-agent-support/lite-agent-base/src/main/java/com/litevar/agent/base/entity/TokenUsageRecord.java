package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author reid
 * @since 2025/12/18
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CollectionName("token_usage")
public class TokenUsageRecord {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    @MongoIndex
    private String userId;
    /**
     * 模型ID
     */
    private String modelId;
    /**
     * agent ID
     */
    private String agentId;
    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 输入token数
     */
    private Integer promptTokens;
    /**
     * 输出token数
     */
    private Integer completionTokens;
    /**
     * 总token数
     */
    private Integer totalTokens;
    /**
     * 消耗的积分数
     */
    private BigDecimal usedPoints;
    /**
     * 记录日期字符串，格式yyyy-MM-dd
     */
    private String dateStr;

    @MongoIndex
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @CollectionField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
