package com.litevar.agent.base.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author uncle
 * @since 2024/7/12 10:51
 */
@Component
public class RedisUtil {
    private static RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
    }

    public static Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public static void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public static void setValue(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    public static void delKey(String key) {
        redisTemplate.delete(key);
    }

    /**
     * list结构
     */
    public static <T> void setListValue(String key, T[] values) {
        redisTemplate.opsForList().rightPushAll(key, values);
    }

    public static List<Object> getListValue(String key) {
        //-1表示到List的末尾位置
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public static Long getKeyTtl(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public static Boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取指定前缀的key
     */
    public static Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public static Boolean setNx(String key, Object value) {
        if (StrUtil.isEmpty(key)) {
            return Boolean.FALSE;
        }
        BoundValueOperations<String, Object> boundValueOps = redisTemplate.boundValueOps(key);
        return boundValueOps.setIfAbsent(value);
    }

    public static void setHashValue(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public static void setHashValue(String key, Map<String, Object> value) {
        redisTemplate.opsForHash().putAll(key, value);
    }

    public static void setHashValue(String key, Map<String, Object> value, long ttl, TimeUnit unit) {
        redisTemplate.opsForHash().putAll(key, value);
        redisTemplate.expire(key, ttl, unit);
    }

    public static Object getHashValue(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }
}
