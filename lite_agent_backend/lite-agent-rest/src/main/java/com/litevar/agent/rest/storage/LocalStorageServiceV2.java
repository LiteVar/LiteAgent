package com.litevar.agent.rest.storage;

import cn.hutool.core.io.FileUtil;
import com.litevar.agent.core.module.storage.StorageServiceV2;
import com.litevar.agent.rest.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author reid
 * @since 2026/1/23
 */

@Slf4j
@Service
@ConditionalOnProperty(value = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceV2 implements StorageServiceV2 {

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public String writeFile(String fileKey, byte[] bytes) {
        File file = resolveTargetPath(fileKey).toFile();
        FileUtil.writeBytes(bytes, file);
        return file.getAbsolutePath();
    }

    @Override
    public String writeFile(String fileKey, InputStream inputStream) {
        File file = resolveTargetPath(fileKey).toFile();
        FileUtil.writeFromStream(inputStream, file);
        return file.getAbsolutePath();
    }

    @Override
    public byte[] readFile(String fileKey) {
        return FileUtil.readBytes(resolveTargetPath(fileKey).toFile());
    }

    @Override
    public InputStream readFileStream(String fileKey) {
        try {
            return Files.newInputStream(resolveTargetPath(fileKey));
        } catch (IOException e) {
            throw new UncheckedIOException("读取文件失败", e);
        }
    }

    /**
     * 下载文件到指定路径
     *
     * @param fileKey   文件key
     * @param targetDir 目标目录
     * @return 目标文件路径
     */
    @Override
    public Path downloadFile(String fileKey, String targetDir) {
        Path source = Path.of(storageProperties.getBasePath(), fileKey);
        Path target = Path.of(targetDir).resolve(source.getFileName());

        if (source.equals(target)) {
            return source;
        }

        FileUtil.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /**
     * 复制目录下所有文件(不包括子目录)到目标路径
     *
     * @param dirKey    目录key
     * @param targetDir 目标目录
     */
    @Override
    public void downloadDir(String dirKey, String targetDir) {
        Path sourceDir = Path.of(storageProperties.getBasePath(), dirKey);
        if (sourceDir.toString().equals(targetDir)) {
            return;
        }

        try {
            FileUtil.copyFilesFromDir(sourceDir.toFile(), Path.of(targetDir).toFile(), true);
        } catch (Exception e) {
            log.error("复制目录失败", e);
            throw new RuntimeException("复制目录失败", e);
        }
    }

    @Override
    public Map<String, byte[]> downloadDir(String dirKey) {
        Path sourceDir = Path.of(storageProperties.getBasePath(), dirKey);
        if (!sourceDir.toFile().exists()) {
            return Collections.emptyMap();
        }

        //读取此目录下的所有文件
        try (Stream<Path> stream = Files.list(sourceDir)) {
            Map<String, byte[]> result = new HashMap<>();
            stream.filter(Files::isRegularFile)
                    .forEach(path -> result.put(path.getFileName().toString(), FileUtil.readBytes(path.toFile())));
            return result;
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    public List<String> listFileKeys(String dirKey) {
        Path sourceDir = Path.of(storageProperties.getBasePath(), dirKey);
        if (!sourceDir.toFile().exists()) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.walk(sourceDir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(path -> {
                    String relative = sourceDir.relativize(path).toString().replace('\\', '/');
                    return Path.of(dirKey, relative).toString().replace('\\', '/');
                })
                .sorted()
                .toList();
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        Path path = Path.of(storageProperties.getBasePath(), fileKey);
        FileUtil.del(path);
    }

    @Override
    public void deleteDir(String fileKey) {
        File file = Path.of(storageProperties.getBasePath()).resolve(fileKey).getParent().toFile();
        FileUtil.del(file);
    }

    private Path resolveTargetPath(String fileKey) {
        Path basePath = Path.of(storageProperties.getBasePath());
        return basePath.resolve(fileKey).normalize();
    }
}
