package com.litevar.agent.runner.service;

import com.litevar.agent.runner.config.RunnerProperties;
import com.litevar.agent.runner.security.CryptoService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Runner pairing service.
 * Manages the shared key for authentication.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class PairingService {

    private final RunnerProperties properties;
    private final CryptoService cryptoService;
    private String sharedKey;

    public PairingService(RunnerProperties properties, CryptoService cryptoService) {
        this.properties = properties;
        this.cryptoService = cryptoService;
    }

    @PostConstruct
    public void init() {
        Path keyFile = Path.of(properties.getDataDir(), "encryptedKey.txt");
        try {
            if (Files.exists(keyFile)) {
                String content = Files.readString(keyFile, StandardCharsets.UTF_8).trim();
                if (StringUtils.hasText(content)) {
                    this.sharedKey = cryptoService.decryptSharedKey(content);
                    return;
                }
            } else {
                Files.createDirectories(keyFile.getParent());
            }
            // Generate new key
            String newSharedKey = generateRandomKey(16);
            String encrypted = cryptoService.encryptSharedKey(newSharedKey);
            Files.writeString(keyFile, encrypted, StandardCharsets.UTF_8);
            this.sharedKey = newSharedKey;
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, "failed to init key: " + ex.getMessage());
        }
    }

    public boolean isKeyReady() {
        return StringUtils.hasText(sharedKey);
    }

    public String getSharedKey() {
        if (!isKeyReady()) {
            throw new RunnerException(RunnerErrorCode.KEY_NOT_READY, null);
        }
        return sharedKey;
    }

    private String generateRandomKey(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}