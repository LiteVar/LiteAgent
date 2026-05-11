package com.litevar.agent.runner.service;

import com.litevar.agent.runner.config.RunnerProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

/**
 * Plugin package storage.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class PackageService {

    private final Path uploadDir;

    public PackageService(RunnerProperties properties) {
        this.uploadDir = Path.of(properties.getUploadDir());
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public Path savePackage(String pluginId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, "file is empty");
        }
        String safePluginId = normalizePluginId(pluginId);
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            filename = "image.tar";
        }
        filename = Path.of(filename).getFileName().toString();
        if (".".equals(filename) || "..".equals(filename)) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, "invalid filename");
        }
        Path pluginDir = uploadDir.resolve(safePluginId);
        try {
            Files.createDirectories(pluginDir);
            Path target = pluginDir.resolve(filename);
            file.transferTo(target);
            return target;
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public Path getLatestPackage(String pluginId) {
        Path pluginDir = uploadDir.resolve(normalizePluginId(pluginId));
        if (!Files.exists(pluginDir)) {
            throw new RunnerException(RunnerErrorCode.PACKAGE_NOT_FOUND, "no package uploaded");
        }
        try (var stream = Files.list(pluginDir)) {
            Optional<Path> latest = stream.filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
            if (latest.isEmpty()) {
                throw new RunnerException(RunnerErrorCode.PACKAGE_NOT_FOUND, "no package uploaded");
            }
            return latest.get();
        } catch (IOException ex) {
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    public boolean hasPackage(String pluginId) {
        Path pluginDir = uploadDir.resolve(normalizePluginId(pluginId));
        if (!Files.exists(pluginDir)) {
            return false;
        }
        try (var stream = Files.list(pluginDir)) {
            return stream.anyMatch(Files::isRegularFile);
        } catch (IOException ex) {
            return false;
        }
    }

    public void downloadPackage(String pluginId, String url) {
        if (!StringUtils.hasText(url)) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, "url is empty");
        }
        String safePluginId = normalizePluginId(pluginId);
        Path pluginDir = uploadDir.resolve(safePluginId);
        try {
            Files.createDirectories(pluginDir);
            Path target = pluginDir.resolve("image.tar");

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofFile(target));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RunnerException(RunnerErrorCode.INTERNAL_ERROR, "download failed: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new RunnerException(RunnerErrorCode.INVALID_PARAM, "invalid url");
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
