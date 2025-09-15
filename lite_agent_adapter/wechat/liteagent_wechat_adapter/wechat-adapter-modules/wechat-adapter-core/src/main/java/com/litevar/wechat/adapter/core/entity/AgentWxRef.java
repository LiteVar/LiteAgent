package com.litevar.wechat.adapter.core.entity;
import com.litevar.wechat.adapter.common.mongoplus.entity.BaseEntity;
import com.litevar.wechat.adapter.core.dto.AgentWxRefDTO;
import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.IdTypeEnum;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 *  agent与服务号绑定对象
 * @author Teoan
 * @since 2025/8/13 17:11
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@CollectionName("agent_wx_ref")
@AutoMapper(target = AgentWxRefDTO.class)
public class AgentWxRef extends BaseEntity implements Serializable {

    /**
     * 唯一标识
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;




    /**
     * 关系名称
     */
    @MongoIndex
    private String name;



    /**
     * AgentApiKey
     */
    @MongoIndex
    private String agentApiKey;


    /**
     * AgentBaseUrl
     */
    private String agentBaseUrl;


    /**
     * 服务号的appId
     */
    @MongoIndex(unique = true)
    private String appId;

    /**
     * 服务号的appSecret
     */
    private String appSecret;


    /**
     * 服务号的 token
     */
    private String token;


    /**
     * 服务号的 aesKey
     */
    private String aesKey;


}
