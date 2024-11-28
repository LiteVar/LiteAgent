package com.litevar.agent.rest.controller.v1;

import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.service.AuthService;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.agent.LocalAgentService;
import com.litevar.agent.core.module.workspace.WorkspaceService;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 鉴权
 *
 * @author uncle
 * @since 2024/7/3 15:06
 */
@Validated
@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private LocalAgentService localAgentService;

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
     * 初始化系统的登录
     *
     * @param email    邮箱
     * @param password 密码
     * @param username 名字
     * @return
     */
    @IgnoreAuth
    @PostMapping("/loginOfInit")
    public ResponseData<String> login(@RequestParam("email") @Email String email, @RequestParam("password") String password, @RequestParam("username") String username) {
        Object value = RedisUtil.getValue(CacheKey.INIT_STATUS);
        if (value != null) {
            throw new ServiceException(400, "系统已初始化,请勿重复初始化");
        }

        //新增用户,为用户创建默认工作空间
        String userId = workspaceService.addUser(username, password, email);
        String workspaceId = workspaceService.addWorkspace(email + "'s workspace");
        workspaceService.addMemberToWorkspace(userId, email, workspaceId, RoleEnum.ROLE_ADMIN.getCode());

        RedisUtil.setValue(CacheKey.INIT_STATUS, 1);

        String token = authService.login(email, password);
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
     * 系统初始化状态
     * 1-已初始化,0-未初始化
     *
     * @return
     */
    @IgnoreAuth
    @GetMapping("/initStatus")
    public ResponseData<Integer> initStatus() {
        Object value = RedisUtil.getValue(CacheKey.INIT_STATUS);
        int status = value == null ? 0 : 1;
        return ResponseData.success(status);
    }
}
