package com.litevar.dingtalk.adapter.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.litevar.dingtalk.adapter.core.dto.RobotPermissionsDTO;
import com.litevar.dingtalk.adapter.core.entity.Department;
import com.litevar.dingtalk.adapter.core.entity.RobotPermissions;
import com.litevar.dingtalk.adapter.core.entity.User;
import com.litevar.dingtalk.adapter.core.service.DepartmentService;
import com.litevar.dingtalk.adapter.core.service.IRobotPermissionsService;
import com.mongoplus.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Teoan
 * @since 2025/7/24 11:46
 */
@Service
@RequiredArgsConstructor
public class RobotPermissionsServiceImpl extends ServiceImpl<RobotPermissions> implements IRobotPermissionsService {


    private final DepartmentService departmentService;

    private final Converter converter;

    /**
     * 删除robot权限
     *
     * @param id
     */
    @Override
    public void deleteRobotPermission(String id) {
        removeById(id);
    }

    /**
     * 更新robot权限
     *
     * @param robotPermissionsDTO
     */
    @Override
    public void updateRobotPermission(RobotPermissionsDTO robotPermissionsDTO) {
        RobotPermissions convert = converter.convert(robotPermissionsDTO, RobotPermissions.class);
        updateById(convert);
    }

    /**
     * 获取robot权限
     *
     * @param robotCode
     * @return
     */
    @Override
    public RobotPermissionsDTO getRobotPermissions(String robotCode) {
        RobotPermissions robotPermissions = lambdaQuery().eq(RobotPermissions::getRobotCode, robotCode).one();

        return converter.convert(robotPermissions, RobotPermissionsDTO.class);
    }

    /**
     * 创建robot权限
     *
     * @param robotPermissionsDTO
     */
    @Override
    public void createRobotPermission(RobotPermissionsDTO robotPermissionsDTO) {
        RobotPermissions convert = converter.convert(robotPermissionsDTO, RobotPermissions.class);
        save(convert);
    }


    /**
     * 检查robot权限
     *
     * @param robotCode 机器人编码
     * @param userId    用户id
     */
    @Override
    public Boolean checkRobotPermission(String robotCode, String userId) {
        RobotPermissionsDTO robotPermissions = this.getRobotPermissions(robotCode);
        if(ObjUtil.isEmpty(robotPermissions)){
            return true;
        }
        List<Long> parentDeptListByUserId = departmentService.getParentDeptListByUserId(userId,robotCode);
        List<Long> departmentIdList = Optional.ofNullable(robotPermissions.getDepartmentList()).orElse(List.of()).stream().map(Department::getDepartmentId).toList();
        List<String> userIdList = Optional.ofNullable(robotPermissions.getUserList()).orElse(List.of()).stream().map(User::getUserId).toList();
        // 先检查部门权限 有交集说明有权限
        Collection<Long> intersection = CollUtil.intersection(parentDeptListByUserId, departmentIdList);
        if(CollUtil.isNotEmpty(intersection)){
            return true;
        }
        return CollUtil.anyMatch(userIdList, userId::equals);
    }
}