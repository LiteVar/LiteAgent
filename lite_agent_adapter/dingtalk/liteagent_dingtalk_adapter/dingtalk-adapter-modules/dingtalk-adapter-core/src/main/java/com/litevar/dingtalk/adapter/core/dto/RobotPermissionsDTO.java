package com.litevar.dingtalk.adapter.core.dto;

import com.litevar.dingtalk.adapter.core.entity.Department;
import com.litevar.dingtalk.adapter.core.entity.User;
import com.mongoplus.annotation.ID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent权限信息DTO
 *
 * @author Teoan
 * @since 2025/8/13
 */
@Data
@Schema(description = "Robot权限信息DTO")
public class RobotPermissionsDTO {

    /**
     * 权限ID
     */
    @Schema(description = "权限ID")
    @ID
    private String id;

    /**
     * robotCode
     */
    @Schema(description = "robotCode")
    @NotBlank(message = "robotCode不能为空")
    private String robotCode;

    /**
     * 用户名称列表
     */
    @Schema(description = "用户列表")
    private List<User> userList;


    /**
     * 部门ID列表
     */
    @Schema(description = "部门列表")
    private List<Department> departmentList;


    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
