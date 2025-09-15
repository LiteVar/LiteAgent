package com.litevar.wechat.adapter.common.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录用户信息
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
     * 用户邮箱
     */
    private String email;

    /**
     * 用户登录账号
     */
    private String loginId;


    /**
     * 用户手机号
     */
    private String mobile;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 备注信息
     */
    private String remark;


    /**
     * 用户userId
     */
    private String userId;




}