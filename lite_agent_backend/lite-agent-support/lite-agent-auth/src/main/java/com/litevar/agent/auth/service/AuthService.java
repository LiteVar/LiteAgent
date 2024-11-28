package com.litevar.agent.auth.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.jwt.JWT;
import com.litevar.agent.base.vo.LoginUser;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.enums.AccountStatus;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.AccountRepository;
import com.litevar.agent.base.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author uncle
 * @since 2024/7/31 10:12
 */
@Service
public class AuthService {

    @Autowired
    private AccountRepository accountRepository;

    public String login(String email, String password) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            throw new ServiceException(ServiceExceptionEnum.WRONG_ACCOUNT);
        }
        if (!account.getStatus().equals(AccountStatus.ACTIVE.getValue())) {
            throw new ServiceException(ServiceExceptionEnum.BAN_ACCOUNT);
        }

        String encryptPwd = SecureUtil.md5(account.getSalt() + password);
        if (!StrUtil.equals(encryptPwd, account.getPassword())) {
            throw new ServiceException(ServiceExceptionEnum.WRONG_PASSWORD);
        }
        LoginUser loginUser = buildLoginUser(account.getId(), account.getName(), account.getEmail());
        //7天过期
        int expire = 1000 * 3600 * 24 * 7;
        Date expirationDate = new Date(System.currentTimeMillis() + expire);
        String token = JWT.create()
                .addPayloads(BeanUtil.beanToMap(loginUser))
                .setKey(CommonConstant.JWT_SECRET.getBytes())
                .setExpiresAt(expirationDate)
                .sign();
        RedisUtil.setValue(String.format(CacheKey.LOGIN_TOKEN, loginUser.getUuid()), token, expire, TimeUnit.MILLISECONDS);

        return token;
    }

    private LoginUser buildLoginUser(String userId, String username, String email) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUuid(IdUtil.simpleUUID());
        loginUser.setId(userId);
        loginUser.setUsername(username);
        loginUser.setEmail(email);
        return loginUser;
    }
}
