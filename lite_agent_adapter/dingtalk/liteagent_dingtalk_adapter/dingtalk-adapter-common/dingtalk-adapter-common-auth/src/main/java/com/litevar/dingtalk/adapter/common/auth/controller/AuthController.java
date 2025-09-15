package com.litevar.dingtalk.adapter.common.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.SaTokenInfo;
import com.litevar.dingtalk.adapter.common.auth.service.AuthService;
import com.litevar.dingtalk.adapter.common.auth.utils.LoginUtil;
import com.litevar.dingtalk.adapter.common.core.web.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 登录认证控制器
 *
 * @author Teoan
 * @since 2025/7/25 10:55
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "用户登录", description = "用户登录操作接口")
public class AuthController {

    private final AuthService authService;


    /**
     * 用户登录
     */
    @GetMapping("/login")
    @Operation(summary = "用户登录", description = "管理员用户登陆")
    @SaIgnore
    public R<SaTokenInfo> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        return R.ok(authService.adminLogin(username, password));
    }


    /**
     * 获取用户信息
     */
    @GetMapping("/user/info")
    @Operation(summary = "获取当前登录用户信息", description = "获取用户信息")
    @SaCheckLogin
    public R<?> getUserInfo() {
        return R.ok(LoginUtil.getCurrentUser());
    }


    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @SaCheckLogin
    public R<?> refreshToken() {
        authService.refreshToken();
        return R.ok();
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    @SaCheckLogin
    public R<?> logout() {
        authService.logout();
        return R.ok();
    }


}