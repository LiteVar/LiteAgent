package com.litevar.agent.auth.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.auth.util.JwtUtil;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.enums.AccountStatus;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.vo.LoginUser;
import com.mongoplus.mapper.BaseMapper;
import com.mongoplus.support.SFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author uncle
 * @since 2024/7/31 10:12
 */
@Service
public class AuthService {
    @Autowired
    private BaseMapper baseMapper;

    public String login(String email, String password) {
        SFunction<Account, String> getEmail = Account::getEmail;
        List<Account> accounts = baseMapper.getByColumn(getEmail.getFieldNameLine(), email, Account.class);
        if (accounts.isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.WRONG_ACCOUNT);
        }
        Account account = accounts.get(0);
        if (!account.getStatus().equals(AccountStatus.ACTIVE.getValue())) {
            throw new ServiceException(ServiceExceptionEnum.BAN_ACCOUNT);
        }

        String encryptPwd = SecureUtil.md5(account.getSalt() + password);
        if (!StrUtil.equals(encryptPwd, account.getPassword())) {
            throw new ServiceException(ServiceExceptionEnum.WRONG_PASSWORD);
        }

        LoginUser loginUser = LoginUser.build(account.getId(), account.getName(), account.getEmail());
        return JwtUtil.createToken(loginUser);
    }
}
