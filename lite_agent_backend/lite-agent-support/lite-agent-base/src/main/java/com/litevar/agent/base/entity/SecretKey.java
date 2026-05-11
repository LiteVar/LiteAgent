package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

/**
 * 密钥
 *
 * @author uncle
 * @since 2026/1/5 20:25
 */
@Data
@CollectionName("secret_key")
public class SecretKey {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * key使用场景
     */
    private String type;

    /**
     * 密钥
     */
    private String key;
}
