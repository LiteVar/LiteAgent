package com.litevar.wechat.adapter.common.auth.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.hutool.core.util.StrUtil;
import com.litevar.wechat.adapter.common.auth.entity.LoginUser;
import com.litevar.wechat.adapter.common.auth.utils.LoginUtil;
import com.litevar.wechat.adapter.common.core.config.WechatAdapterAdminProperties;
import com.litevar.wechat.adapter.common.core.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证服务层
 *
 * @author Teoan
 * @since 2025/8/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WechatAdapterAdminProperties adminProperties;

    /**
     * 用户登录
     */
    public SaTokenInfo adminLogin(String username, String password) {

        if(StrUtil.equals(username,adminProperties.getUsername())
                && StrUtil.equals(password,adminProperties.getPassword())){
            // 临时管理员登陆，后续对接liteagent
            LoginUser loginUser = new LoginUser();
            loginUser.setAdmin(true);
            loginUser.setLoginId(username);
            loginUser.setUserId(username);
            LoginUtil.login(loginUser);
            return LoginUtil.getTokenInfo();
        }else {
            throw new AuthException("用户名或密码错误");
        }
    }


    /**
     * 刷新令牌
     */
    public void refreshToken() {
        LoginUtil.refreshToken();
    }

    /**
     * 用户登出
     */
    public void logout() {
        LoginUtil.logout();
    }


}