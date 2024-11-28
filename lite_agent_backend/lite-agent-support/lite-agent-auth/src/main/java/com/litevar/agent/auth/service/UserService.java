package com.litevar.agent.auth.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.AccountRepository;
import com.litevar.agent.base.util.LoginContext;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author uncle
 * @since 2024/8/29 18:53
 */
@Service
public class UserService {
    @Autowired
    private AccountRepository accountRepository;

    @Cacheable(value = CacheKey.USER_INFO, key = "#id")
    public Account getById(String id) {
        return accountRepository.findById(id).orElseThrow();
    }

    @CacheEvict(value = CacheKey.USER_INFO, key = "#userId")
    public void update(String userId, String name, String avatar) {
        Account account = getById(userId);
        if (!StrUtil.equals(name, account.getName())) {
            account.setName(name);
        }
        if (!StrUtil.equals(avatar, account.getAvatar())) {
            account.setAvatar(avatar);
        }

        accountRepository.save(account);
    }

    @CacheEvict(value = CacheKey.USER_INFO, key = "#result.id")
    public Account update(String originPwd, String newPwd) {
        Account account = getById(LoginContext.currentUserId());
        String old = SecureUtil.md5(account.getSalt() + originPwd);
        if (!StrUtil.equals(old, account.getPassword())) {
            throw new ServiceException(ServiceExceptionEnum.ORIGIN_PASSWORD_WRONG);
        }
        String password = SecureUtil.md5(account.getSalt() + newPwd);
        account.setPassword(password);
        return accountRepository.save(account);
    }

    private UserService proxy() {
        return (UserService) AopContext.currentProxy();
    }
}
