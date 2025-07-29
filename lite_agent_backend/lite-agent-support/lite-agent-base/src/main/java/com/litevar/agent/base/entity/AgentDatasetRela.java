package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoCompoundIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent dataset 关联表
 *
 * @author reid
 * @since 3/10/25
 */

@Data
@CollectionName("agent_dataset_rela")
@MongoCompoundIndex(value = "{'$agentId': 1,'$datasetId': 1}", unique = true)
public class AgentDatasetRela {
    /**
     * id
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String agentId;

    private String datasetId;

    /**
     * 创建时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
