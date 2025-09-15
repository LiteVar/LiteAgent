package com.litevar.dingtalk.adapter.common.auth.entity;

import cn.dev33.satoken.session.SaSession;
import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.IdTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author Teoan
 * @since 2025/7/28 09:44
 */
@Data
@Builder
@CollectionName("sa_token_mongo")
@AllArgsConstructor
@NoArgsConstructor
public class SaTokenMongoData {

    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    // token
    @MongoIndex(unique = true)
    private String key;

    // sa-token 的 session
    private SaSession session;

    // sa-token 的 token string
    private String string;

    // 给 expireAt 添加 `@Indexed(expireAfterSeconds = 0)` 注解，当过期时MongoDB会自动帮我删除过期的数据
    @MongoIndex(expireAfterSeconds = 0)
    private LocalDateTime expireAt;

}