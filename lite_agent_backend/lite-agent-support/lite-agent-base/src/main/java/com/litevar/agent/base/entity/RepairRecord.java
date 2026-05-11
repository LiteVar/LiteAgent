package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据修复记录
 *
 * @author uncle
 * @since 2026/1/15 16:54
 */
@Data
@CollectionName("repair_record")
public class RepairRecord {
    /**
     * 主键
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 修复任务标识
     */
    @MongoIndex(unique = true)
    private String repairKey;

    /**
     * 修复版本
     */
    private String version;

    /**
     * 执行时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
