package com.litevar.dingtalk.adapter.core.entity;

import com.litevar.dingtalk.adapter.common.mongoplus.entity.BaseEntity;
import com.litevar.dingtalk.adapter.core.dto.RobotPermissionsDTO;
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
import java.util.List;

/**
 *  agent 权限对象
 * @author Teoan
 * @since 2025/8/13 17:11
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@CollectionName("robot_permissions")
@AutoMapper(target = RobotPermissionsDTO.class)
public class RobotPermissions extends BaseEntity implements Serializable {

    /**
     * 唯一标识
     */
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * agentApiKey agent唯一标识
     */
    @MongoIndex
    private String robotCode;


    /**
     * 有权限使用的部门列表
     */
    private List<Department> departmentList;


    /**
     * 有权限使用的用户列表
     */
    private List<User> userList;



}
