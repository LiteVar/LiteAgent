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
 * 工作空间成员
 *
 * @author uncle
 * @since 2024/8/1 11:58
 */
@Data
@CollectionName("workspace_member")
public class WorkspaceMember {

    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;
    /**
     * 空间id
     */
    @MongoIndex
    private String workspaceId;
    /**
     * 成员用户id
     */
    @MongoIndex
    private String userId;

    /**
     * 成员账号
     */
    private String email;
    /**
     * 空间成员角色
     */
    private Integer role;
    /**
     * 加入时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";
}