package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间
 *
 * @author uncle
 * @since 2024/8/1 11:12
 */
@Data
@CollectionName("workspace")
public class Workspace {

    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;
    /**
     * 工作空间名字
     */
    @MongoIndex
    private String name;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";
}
