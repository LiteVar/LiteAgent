package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.service.UserService;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.util.MailSendUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

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
    @Autowired
    private MailSendUtil mailSendUtil;

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

    /**
     * 重置密码-验证邮箱
     *
     * @param email 邮箱
     * @return
     */
    @IgnoreAuth
    @PostMapping("/resetPwd/captcha")
    public ResponseData<String> resetPwdCaptcha(@Email @RequestParam("email") String email) {
        Account account = userService.getByEmail(email);

        //账号存在,发送验证码到邮箱
        String captcha = RandomUtil.randomString(6);
        String emailContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset='UTF-8'>
                </head>
                <body>
                    <p>您好，</p>
                    <p>我们收到了您在LiteAgent的密码重置请求。</p>
                    <p>请复制以下验证码以重置密码（30分钟内有效）：</p>
                    <span style='font-size: 24px; font-weight: bold; color: #333; letter-spacing: 3px;'>%s</span>
                    <p>如果您并未请求重置密码，请忽略此邮件。</p>
                    <br>
                    <p>此致，<br>LiteAgent团队</p>
                </body>
                </html>
                """.formatted(captcha);
        mailSendUtil.sendHtml(account.getEmail(), "LiteAgent密码重置验证码", emailContent);

        String key = String.format(CacheKey.RESET_PASSWORD_CAPTCHA, MD5.create().digestHex(account.getEmail()));
        RedisUtil.setValue(key, captcha, 30, TimeUnit.MINUTES);

        return ResponseData.success();
    }

    /**
     * 重置密码-检查验证码
     *
     * @param email
     * @param captcha
     * @return
     */
    @IgnoreAuth
    @PostMapping("/resetPwd/captcha/verify")
    public ResponseData<String> resetPwdCaptchaVerify(
            @Email @RequestParam String email,
            @NotBlank @RequestParam String captcha
    ) {
        String key = String.format(CacheKey.RESET_PASSWORD_CAPTCHA, MD5.create().digestHex(email));
        Object value = RedisUtil.getValue(key);
        if (value == null || !value.equals(captcha)) {
            return ResponseData.of(ServiceExceptionEnum.CAPTCHA_EXPIRED);
        }

        return ResponseData.success();
    }

    /**
     * 重置密码
     *
     * @param email
     * @param password
     * @return
     */
    @IgnoreAuth
    @PostMapping("/resetPwd/confirm")
    public ResponseData<String> resetPwdConfirm(
            @Email @RequestParam String email,
            @NotBlank @RequestParam String password
    ) {
        String key = String.format(CacheKey.RESET_PASSWORD_CAPTCHA, MD5.create().digestHex(email));
        Object value = RedisUtil.getValue(key);
        if (value == null) {
            return ResponseData.of(ServiceExceptionEnum.CAPTCHA_EXPIRED);
        }

        userService.resetPassword(email, password);
        return ResponseData.success();
    }
}
