package com.litevar.dingtalk.adapter.core.entity;

import com.litevar.dingtalk.adapter.common.mongoplus.entity.BaseEntity;
import com.litevar.dingtalk.adapter.core.dto.AgentRobotRefDTO;
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
 *  agent与机器人绑定对象
 * @author Teoan
 * @since 2025/8/13 17:11
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@CollectionName("agent_robot_ref")
@AutoMapper(target = AgentRobotRefDTO.class)
public class AgentRobotRef extends BaseEntity implements Serializable {

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
     * 机器人code
     */
    @MongoIndex(unique = true)
    private String robotCode;

    /**
     * 机器人应用的clientId
     */
    private String robotClientId;


    /**
     * 机器人应用的clientSecret
     */
    private String robotClientSecret;


    /**
     * 卡片模板Id
     */
    private String cardTemplateId;


}
