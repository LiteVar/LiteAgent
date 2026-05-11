package com.litevar.agent.runner.service;

import com.litevar.agent.runner.config.RunnerProperties;
import com.litevar.agent.runner.security.CryptoService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Plugin key file management.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class PluginKeyService {

    private static final String KEY_DIR = "key";
    private static final String KEY_FILENAME = "encryptedKey.txt";

    private final Path dataDir;
    private final CryptoService cryptoService;

    public PluginKeyService(RunnerProperties properties, CryptoService cryptoService) {
        this.dataDir = Path.of(properties.getDataDir());
        this.cryptoService = cryptoService;
    }

    public void ensureKeyFile(String pluginId) {
        String safePluginId = normalizePluginId(pluginId);
        Path keyFile = dataDir.resolve("plugins").resolve(safePluginId).resolve(KEY_DIR).resolve(KEY_FILENAME);
        if (Files.exists(keyFile)) {
            return;
        }
        try {
            Files.createDirectories(keyFile.getParent());
            String sharedKey = cryptoService.deriveSharedKey(safePluginId);
            String encryptedKey = cryptoService.encryptSharedKey(sharedKey);
            Files.writeString(keyFile, encryptedKey, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ex) {
            // key already created by concurrent call
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    private String normalizePluginId(String pluginId) {
        if (!StringUtils.hasText(pluginId)) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, "pluginId missing");
        }
        if (pluginId.contains("..") || pluginId.contains("/") || pluginId.contains("\\")) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, "invalid pluginId");
        }
        return pluginId;
    }
}
