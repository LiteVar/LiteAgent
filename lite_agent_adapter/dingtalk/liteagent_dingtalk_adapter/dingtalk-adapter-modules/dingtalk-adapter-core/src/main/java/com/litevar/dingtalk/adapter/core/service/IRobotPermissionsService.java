package com.litevar.dingtalk.adapter.core.service;


import com.litevar.dingtalk.adapter.core.dto.RobotPermissionsDTO;
import com.litevar.dingtalk.adapter.core.entity.RobotPermissions;
import com.mongoplus.service.IService;

/**
 * @author Teoan
 * @since 2025/7/28 14:33
 */
public interface IRobotPermissionsService extends IService<RobotPermissions> {

    /**
     * 创建robot权限
     * @param robotPermissionsDTO
     */
    void createRobotPermission(RobotPermissionsDTO robotPermissionsDTO);


    /**
     * 获取robot权限
     * @param robotId
     * @return
     */
    RobotPermissionsDTO getRobotPermissions(String robotCode);


    /**
     * 更新robot权限
     * @param robotPermissionsDTO
     */
    void updateRobotPermission(RobotPermissionsDTO robotPermissionsDTO);


    /**
     * 删除robot权限
     * @param id
     */
    void deleteRobotPermission(String id);


    /**
     * 检查robot权限
     * @param robotCode 机器人编码
     * @param userId 用户id
     */
    Boolean checkRobotPermission(String robotCode, String userId);



}