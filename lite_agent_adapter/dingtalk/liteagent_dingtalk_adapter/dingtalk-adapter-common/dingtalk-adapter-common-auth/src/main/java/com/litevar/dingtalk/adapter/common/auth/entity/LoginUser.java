package com.litevar.dingtalk.adapter.common.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录用户信息
 * 该类基于钉钉SDK的OapiV2UserGetResponse类字段定义
 *
 * @author Teoan
 * @since 2025/7/30 18:25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginUser {


    /**
     * 用户是否激活
     */
    private Boolean active;

    /**
     * 是否为企业的管理员
     */
    private Boolean admin;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 是否为企业的老板
     */
    private Boolean boss;

    /**
     * 用户所属部门ID列表
     */
    private List<Long> deptIdList;

    /**
     * 用户在部门中的排序信息
     */
    private List<DeptOrder> deptOrderList;

    /**
     * 用户在部门中的职位信息
     */
    private List<DeptPosition> deptPositionList;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 是否为专属账号
     */
    private Boolean exclusiveAccount;

    /**
     * 专属账号类型
     */
    private String exclusiveAccountType;

    /**
     * 扩展属性
     */
    private String extension;

    /**
     * 是否隐藏手机号
     */
    private Boolean hideMobile;

    /**
     * 入职时间
     */
    private Long hiredDate;

    /**
     * 员工工号
     */
    private String jobNumber;


    /**
     * 用户在部门中的领导信息
     */
    private List<DeptLeader> leaderInDept;

    /**
     * 用户登录账号
     */
    private String loginId;

    /**
     * 用户的直属主管ID
     */
    private String managerUserid;

    /**
     * 用户手机号
     */
    private String mobile;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户企业邮箱
     */
    private String orgEmail;

    /**
     * 企业邮箱类型
     */
    private String orgEmailType;

    /**
     * 是否已完成实名认证
     */
    private Boolean realAuthed;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 用户角色列表
     */
    private List<UserRole> roleList;

    /**
     * 是否为高管
     */
    private Boolean senior;

    /**
     * 手机号国家码
     */
    private String stateCode;

    /**
     * 分机号
     */
    private String telephone;

    /**
     * 职位
     */
    private String title;

    /**
     * 用户unionId
     */
    private String unionId;

    /**
     * 用户userId
     */
    private String userId;

    /**
     * 工作地点
     */
    private String workPlace;


    /**
     * 部门排序信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeptOrder  {

        /**
         * 部门ID
         */
        private Long deptId;

        /**
         * 排序值
         */
        private Long order;

    }

    /**
     * 部门领导信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeptLeader {

        private Long deptId;

        /**
         * 是否为领导
         */
        private Boolean leader;

    }
    /**
     * 部门职位信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeptPosition {

        private Long deptId;

        /**
         * 是否主部门
         */
        private Boolean isMain;

        /**
         * 职位名称
         */
        private String title;

        /**
         * 工作地点
         */
        private String workPlace;
    }

    /**
     * 用户权限信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserRole {
        private String groupName;

        private Long id;

        private String name;
    }


}