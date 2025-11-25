package com.litevar.agent.base.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * 账号表
 *
 * @author reid
 * @since 2024/7/25
 */
@Data
@CollectionName("account")
public class Account {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 昵称
     */
    private String name;

    /**
     * 邮箱
     */
    @MongoIndex
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    @JsonIgnore
    private String password;
    @JsonIgnore
    private String salt;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 账号状态
     */
    private Integer status;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @CollectionField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";
}
