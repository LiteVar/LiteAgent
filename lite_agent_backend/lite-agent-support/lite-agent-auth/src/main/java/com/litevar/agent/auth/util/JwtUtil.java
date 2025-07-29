package com.litevar.agent.auth.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.LoginUser;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author reid
 * @since 4/7/25
 */
public class JwtUtil {
    //默认7天过期
    static int expire = 1000 * 3600 * 24 * 7;

    public static String createToken(LoginUser loginUser) {
        Date expirationDate = new Date(System.currentTimeMillis() + expire);
        String token = JWT.create()
            .addPayloads(BeanUtil.beanToMap(loginUser))
            .setKey(CommonConstant.JWT_SECRET.getBytes())
            .setExpiresAt(expirationDate)
            .sign();

        //将token存入redis
        RedisUtil.setValue(String.format(CacheKey.LOGIN_TOKEN, loginUser.getUuid()), token, expire, TimeUnit.MILLISECONDS);

        return token;
    }

    public static String createToken(LoginUser loginUser, Duration duration) {
        Date expirationDate = new Date(System.currentTimeMillis() + duration.toMillis());
        String token = JWT.create()
            .addPayloads(BeanUtil.beanToMap(loginUser))
            .setKey(CommonConstant.JWT_SECRET.getBytes())
            .setExpiresAt(expirationDate)
            .sign();

        //将token存入redis
        RedisUtil.setValue(String.format(CacheKey.LOGIN_TOKEN, loginUser.getUuid()), token, duration.toMillis(), TimeUnit.MILLISECONDS);

        return token;
    }

    public static String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader(CommonConstant.HEADER_AUTH);
        if (ObjectUtil.isEmpty(token)) {
            return null;
        } else {
            //token不是以Bearer开头，则响应回格式不正确
            if (!token.startsWith(CommonConstant.JWT_TOKEN_PREFIX)) {
                throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
            }
            try {
                token = token.substring(CommonConstant.JWT_TOKEN_PREFIX.length() + 1);
            } catch (StringIndexOutOfBoundsException e) {
                throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
            }
        }
        return token;
    }

    public static String getApikeyFromRequest(HttpServletRequest request) {
        String apikey = request.getHeader(CommonConstant.HEADER_AUTH);
        if (StrUtil.isBlank(apikey)) {
            throw new ServiceException(ServiceExceptionEnum.INVALID_APIKEY);
        }
        if (!apikey.startsWith(CommonConstant.JWT_TOKEN_PREFIX)) {
            throw new ServiceException(ServiceExceptionEnum.INVALID_APIKEY);
        }

        return apikey.substring(CommonConstant.JWT_TOKEN_PREFIX.length() + 1);
    }

}
