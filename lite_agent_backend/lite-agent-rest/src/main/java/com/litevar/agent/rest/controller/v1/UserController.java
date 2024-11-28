package com.litevar.agent.rest.controller.v1;

import com.litevar.agent.auth.service.UserService;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 个人信息管理
 *
 * @author uncle
 * @since 2024/8/29 18:50
 */
@RestController
@RequestMapping("/v1/user")
public class UserController {
    @Autowired
    private UserService userService;

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

    /**
     * 修改用户信息
     *
     * @param name   昵称
     * @param avatar 头像url
     * @return
     */
    @PutMapping("/update")
    public ResponseData<String> updateInfo(@RequestParam("name") String name,
                                           @RequestParam(value = "avatar", required = false) String avatar) {
        userService.update(LoginContext.currentUserId(), name, avatar);
        return ResponseData.success();
    }

    /**
     * 修改密码
     *
     * @param originPwd 旧密码
     * @param newPwd    新密码
     * @return
     */
    @PutMapping("/updatePwd")
    public ResponseData<String> updatePassword(@RequestParam("originPwd") String originPwd,
                                               @RequestParam("newPwd") String newPwd) {
        userService.update(originPwd, newPwd);
        return ResponseData.success();
    }
}
