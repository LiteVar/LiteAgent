package com.litevar.agent.runner.security;

import com.litevar.agent.runner.service.RunnerErrorCode;
import com.litevar.agent.runner.service.RunnerException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Crypto utilities for runner.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Component
public class CryptoService {

    private static final String AES_KEY = "UdYbRx48hZQR8cys";
    private static final int SHARED_KEY_LENGTH = 16;

    public String decryptSharedKey(String encryptedKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedKey);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] plain = cipher.doFinal(decoded);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, ex.getMessage());
        }
    }

    public String encryptSharedKey(String sharedKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(sharedKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public String deriveSharedKey(String pluginId) {
        try {
            String input = AES_KEY + ":" + pluginId;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            String hex = HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
            return hex.substring(0, SHARED_KEY_LENGTH);
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
