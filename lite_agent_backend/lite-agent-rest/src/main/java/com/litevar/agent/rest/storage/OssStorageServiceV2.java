package com.litevar.agent.rest.storage;

import com.litevar.agent.core.module.storage.StorageServiceV2;
import com.litevar.agent.rest.config.StorageProperties;
import com.litevar.agent.rest.util.OssUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author reid
 * @since 2026/1/23
 */

@Slf4j
@Service
@ConditionalOnProperty(value = "storage.type", havingValue = "oss")
public class OssStorageServiceV2 implements StorageServiceV2 {
    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    private OssUtil ossUtil;

    @Override
    public String writeFile(String fileKey, byte[] bytes) {
        Path path = resolveTargetPath(fileKey);
        return ossUtil.putObject(path.toString(), bytes);
    }

    @Override
    public String writeFile(String fileKey, InputStream inputStream) {
        Path path = resolveTargetPath(fileKey);
        return ossUtil.putObject(path.toString(), inputStream);
    }

    @SneakyThrows
    @Override
    public byte[] readFile(String fileKey) {
        Path path = resolveTargetPath(fileKey);
        return ossUtil.getObjectBytes(path.toString());
    }

    @Override
    public InputStream readFileStream(String fileKey) {
        Path path = resolveTargetPath(fileKey);
        return ossUtil.getObjectStream(path.toString());
    }

    @Override
    public Path downloadFile(String fileKey, String targetDir) {
        Path path = resolveTargetPath(fileKey);
        Path target = Path.of(targetDir).resolve(path.getFileName());
        ossUtil.getObject(path.toString(), target);
        return target;
    }

    @Override
    public void downloadDir(String dirKey, String targetDir) {
        Path path = resolveTargetPath(dirKey);
        ossUtil.downloadDir(path.toString(), targetDir);
    }

    @Override
    public Map<String, byte[]> downloadDir(String dirKey) {
        Path path = resolveTargetPath(dirKey);
        String prefix = path.toString();
        try {
            List<String> keys = ossUtil.listObjectKeys(prefix);
            if (keys.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, byte[]> result = new HashMap<>();
            for (String key : keys) {
                String filename = Path.of(key).getFileName().toString();
                result.put(filename, ossUtil.getObjectBytes(key));
            }
            return result;
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    public List<String> listFileKeys(String dirKey) {
        Path path = resolveTargetPath(dirKey);
        String prefix = path.toString();
        String basePath = Path.of(storageProperties.getBasePath()).normalize().toString().replace('\\', '/');
        String basePrefix = basePath + "/";
        try {
            List<String> keys = ossUtil.listObjectKeys(prefix);
            if (keys.isEmpty()) {
                return Collections.emptyList();
            }
            return keys.stream()
                .map(key -> key.replace('\\', '/'))
                .filter(key -> key.startsWith(basePrefix))
                .map(key -> key.substring(basePrefix.length()))
                .sorted()
                .toList();
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Async
    @Override
    public void deleteFile(String fileKey) {
        Path path = resolveTargetPath(fileKey);
        ossUtil.deleteObject(path.toString());
    }

    @Async
    @Override
    public void deleteDir(String fileKey) {
        Path path = Path.of(storageProperties.getBasePath()).resolve(fileKey).getParent();
        ossUtil.deleteDir(path.toString());
    }

    private Path resolveTargetPath(String fileKey) {
        Path basePath = Path.of(storageProperties.getBasePath());
        return basePath.resolve(fileKey).normalize();
    }
}
