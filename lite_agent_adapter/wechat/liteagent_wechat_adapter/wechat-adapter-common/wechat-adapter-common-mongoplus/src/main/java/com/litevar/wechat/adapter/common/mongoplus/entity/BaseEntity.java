package com.litevar.wechat.adapter.common.mongoplus.entity;

import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.enums.FieldFill;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity基类
 *
 * @author Teoan
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 创建者
     */
    @CollectionField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @CollectionField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @CollectionField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}