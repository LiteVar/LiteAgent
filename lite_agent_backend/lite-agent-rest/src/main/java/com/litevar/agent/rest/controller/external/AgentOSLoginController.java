package com.litevar.agent.rest.controller.external;

import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.service.AuthService;
import com.litevar.agent.auth.service.UserService;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.local.LocalAgentService;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AgentOS 登录
 *
 * @author uncle
 * @since 2026/2/11 09:46
 */
@RestController
@RequestMapping("/v1/agentOS")
public class AgentOSLoginController {
    @Autowired
    private AuthService authService;
    @Autowired
    private LocalAgentService localAgentService;
    @Autowired
    private UserService userService;

    /**
     * 登录
     *
     * @param email    邮箱
     * @param password 密码
     * @return
     */
    @IgnoreAuth
    @PostMapping("/login")
    public ResponseData<String> login(@RequestParam("email") @Email String email, @RequestParam("password") String password) {
        String token = authService.login(email, password);
        return ResponseData.success(token);
    }

    /**
     * 刷新token
     *
     * @return
     */
    @PostMapping("/refreshToken")
    public ResponseData<String> refreshToken() {
        String token = authService.refreshToken();
        return ResponseData.success(token);
    }

    /**
     * 退出登录
     *
     * @return
     */
    @PostMapping("/logout")
    public ResponseData<String> logout() {
        try {
            RedisUtil.delKey(String.format(CacheKey.LOGIN_TOKEN, LoginContext.me().getUuid()));
            localAgentService.clearAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseData.success();
    }

    /**
     * 获取当前用户的信息
     *
     * @return
     */
    @GetMapping("/info")
    public ResponseData<Account> userInfo() {
        Account account = userService.getById(LoginContext.currentUserId());
        return ResponseData.success(account);
    }
}
