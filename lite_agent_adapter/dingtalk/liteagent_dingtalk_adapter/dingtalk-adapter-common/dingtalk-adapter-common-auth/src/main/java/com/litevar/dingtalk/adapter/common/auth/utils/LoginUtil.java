package com.litevar.dingtalk.adapter.common.auth.utils;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.convert.Convert;
import com.litevar.dingtalk.adapter.common.auth.entity.DingTalkUserTokenInfo;
import com.litevar.dingtalk.adapter.common.auth.entity.LoginUser;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Teoan
 * @since 2025/7/25 17:09
 */
@Slf4j
public class LoginUtil {

    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String ROLE_KEY = "role";
    public static final String USER_KEY = "userId";
    // 钉钉的个人Token信息
    public static final String DINGTALK_TOKEN_INFO = "dingtalkUserTokenInfo";


    /**
     * 登录
     */
    public static void login(LoginUser user, DingTalkUserTokenInfo dingTalkUserTokenInfo) {
        StpUtil.login(user.getUserId(), new SaLoginParameter()
                .setExtra(LOGIN_USER_KEY, user)
                .setExtra(DINGTALK_TOKEN_INFO, dingTalkUserTokenInfo)
                .setExtra(USER_KEY, user.getUnionId()));
    }

    /**
     * 获取用户登录id
     */
    public static String getCurrentUserId() {
        return Convert.toStr(StpUtil.getLoginId());
    }


    /**
     * 获取当前用户
     */
    public static LoginUser getCurrentUser() {
        return Convert.convert(LoginUser.class, StpUtil.getExtra(LOGIN_USER_KEY));
    }


    /**
     * 判断当前用户是否是管理员
     *
     * @return
     */
    public static Boolean isAdmin() {
        return getCurrentUser().getAdmin();
    }


    /**
     * 登出
     */
    public static void logout() {
        StpUtil.logout();
    }


    /**
     * 获取当前用户名
     */
    public static String getUserName() {
        return getCurrentUser().getName();
    }

    /**
     * 刷新令牌
     */
    public static void refreshToken() {
        StpUtil.updateLastActiveToNow();
    }


    /**
     * 获取Token信息
     */
    public static SaTokenInfo getTokenInfo() {
        return StpUtil.getTokenInfo();
    }



}