package com.litevar.agent.core.module.storage;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.SecretKey;
import com.mongoplus.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/**
 * @author uncle
 * @since 2026/1/5 20:28
 */
@Service
public class SecretKeyService extends ServiceImpl<SecretKey> {
    public static final String secret = "UdYbRx48hZQR8cys";
    public static final String hub_aes_key = "!9cK#2vP$5nQ@8mW&3zL*6xR(1jY)4bD";
    public static final String FILE_READ_KEY = "file_read";
    public static final String LOCAL_FILE_PATH_KEY = "local_file_path";
    public static final String PLUGIN_KEY_PREFIX = "plugin_";

    @Lazy
    @Resource
    private SecretKeyService self;

    public byte[] getSecretKey(String type) {
        String encryptedKey = self.getEncryptedSecretKey(type);
        return decrypt(encryptedKey);
    }

    @Cacheable(value = CacheKey.SECRET_KEY, key = "#type")
    public String getEncryptedSecretKey(String type) {
        SecretKey secretKey = this.lambdaQuery().eq(SecretKey::getType, type).one();
        if (secretKey == null) {
            secretKey = new SecretKey();
            secretKey.setType(type);
            String randomStr = RandomUtil.randomString(16);
            secretKey.setKey(SecureUtil.aes(secret.getBytes(StandardCharsets.UTF_8)).encryptBase64(randomStr));
            this.save(secretKey);
        }
        return secretKey.getKey();
    }

    @Cacheable(value = CacheKey.SECRET_KEY, key = "#type")
    public String getEncryptedSecretKey(String type, Callable<String> task) throws Exception {
        SecretKey secretKey = this.lambdaQuery().eq(SecretKey::getType, type).one();
        if (secretKey == null) {
            secretKey = new SecretKey();
            secretKey.setType(type);
            String str = task.call();
            secretKey.setKey(SecureUtil.aes(secret.getBytes(StandardCharsets.UTF_8)).encryptBase64(str));
            this.save(secretKey);
        }
        return secretKey.getKey();
    }

    public byte[] decrypt(String aesKey, String encrypted) {
        byte[] base64 = Base64.decode(encrypted);
        return SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8)).decrypt(base64);
    }

    public byte[] decrypt(String encrypted) {
        return decrypt(secret, encrypted);
    }
}
