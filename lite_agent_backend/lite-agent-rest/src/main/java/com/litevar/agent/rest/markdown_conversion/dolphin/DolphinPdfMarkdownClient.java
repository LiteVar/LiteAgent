package com.litevar.agent.rest.markdown_conversion.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DolphinPdfMarkdownClient {

    private final WebClient baseWebClient;
    private final DolphinPdfMdProperties properties;
    private final AtomicReference<WebClient> cachedClient = new AtomicReference<>();

    public Path convert(Path pdfPath, Path outputDir, Consumer<String> statusCallback) throws Exception {
        Objects.requireNonNull(pdfPath, "pdfPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        ensureExists(pdfPath);
        Files.createDirectories(outputDir);

        notifyStatus(statusCallback, "Uploading scanned PDF for OCR conversion");
        String taskId = uploadPdf(pdfPath);

        notifyStatus(statusCallback, "Waiting for remote OCR conversion");
        TaskStatus completed = waitForCompletion(taskId, statusCallback);
        if (!completed.status().equalsIgnoreCase("completed")) {
            throw new IllegalStateException("Remote OCR conversion did not complete successfully for task " + taskId);
        }

        String downloadUrl = requireNonBlank(completed.downloadUrl(), "downloadUrl");
        notifyStatus(statusCallback, "Downloading converted markdown archive");
        Path zipFile = downloadArchive(downloadUrl, taskId);

        try {
            notifyStatus(statusCallback, "Extracting markdown artifacts");
            Path markdownFile = extractArchive(zipFile, outputDir);
            if (markdownFile == null) {
                throw new IllegalStateException("Remote archive does not contain any markdown file for task " + taskId);
            }
            return markdownFile;
        } finally {
            safeDelete(zipFile);
        }
    }

    private String uploadPdf(Path pdfPath) throws IOException {
        WebClient client = resolvedClient();
        org.springframework.util.MultiValueMap<String, Object> data = buildMultipart(pdfPath);
        Mono<JsonNode> response = client.post()
            .uri("/pdfmd/upload-pdf")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(data))
            .retrieve()
            .bodyToMono(JsonNode.class);
        JsonNode node = block(response, properties.getRequestTimeout());
        if (node == null) {
            throw new IllegalStateException("Empty response from upload endpoint");
        }
        String taskId = node.path("task_id").asText(null);
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalStateException("Upload response missing task_id: " + node);
        }
        return taskId;
    }

    private TaskStatus waitForCompletion(String taskId, Consumer<String> statusCallback) throws InterruptedException {
        Duration timeout = properties.getPollTimeout();
        Duration interval = properties.getPollInterval();
        Instant start = Instant.now();
        while (true) {
            TaskStatus status = fetchStatus(taskId);
            if (status == null) {
                throw new IllegalStateException("Unable to fetch task status for " + taskId);
            }
            if ("completed".equalsIgnoreCase(status.status()) || "failed".equalsIgnoreCase(status.status())) {
                return status;
            }
            notifyStatus(statusCallback, status.progress() == null ? "Waiting remote conversion..." : status.progress());
            if (timeout != null && Instant.now().isAfter(start.plus(timeout))) {
                throw new IllegalStateException("Timed out waiting for OCR conversion of task " + taskId);
            }
            Thread.sleep(interval != null ? interval.toMillis() : 3000L);
        }
    }

    private TaskStatus fetchStatus(String taskId) {
        WebClient client = resolvedClient();
        Mono<JsonNode> response = client.get()
            .uri("/pdfmd/task-status/{taskId}", taskId)
            .retrieve()
            .bodyToMono(JsonNode.class);
        JsonNode node = block(response, properties.getRequestTimeout());
        if (node == null) {
            return null;
        }
        String status = node.path("status").asText(null);
        String progress = node.path("progress").asText(null);
        String downloadUrl = node.path("download_url").asText(null);
        return new TaskStatus(status, downloadUrl, progress);
    }

    private Path downloadArchive(String downloadUrl, String taskId) throws IOException {
        WebClient client = resolvedClient();
        Path zipFile = Files.createTempFile("dolphin-" + taskId + "-", ".zip");
        Mono<Void> writeMono = DataBufferUtils.write(
                client.get()
                    .uri(downloadUrl)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class),
                zipFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        ).then();
        try {
            block(writeMono, properties.getDownloadTimeout());
        } catch (RuntimeException ex) {
            safeDelete(zipFile);
            throw new IOException("Failed to download archive for task " + taskId, ex);
        }
        if (Files.size(zipFile) <= 0) {
            safeDelete(zipFile);
            throw new IllegalStateException("Downloaded archive is empty for task " + taskId);
        }
        return zipFile;
    }

    private Path extractArchive(Path zipFile, Path outputDir) throws IOException {
        Path markdownFile = null;
        try (InputStream in = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains("..")) {
                    throw new IOException("Invalid zip entry name: " + entry.getName());
                }
                String normalizedName = entry.getName().replace('\\', '/');
                Path target = outputDir.resolve(normalizedName).normalize();
                if (!target.startsWith(outputDir)) {
                    throw new IOException("Zip entry attempts to escape target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    String lower = target.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (markdownFile == null && lower.endsWith(".md")) {
                        markdownFile = target;
                    }
                }
                zis.closeEntry();
            }
        }
        return markdownFile;
    }

    private MultiValueMap<String, Object> buildMultipart(Path pdfPath) {
        org.springframework.util.LinkedMultiValueMap<String, Object> data = new org.springframework.util.LinkedMultiValueMap<>();
        FileSystemResource resource = new FileSystemResource(pdfPath);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        HttpEntity<FileSystemResource> filePart = new HttpEntity<>(resource, headers);
        data.add("file", filePart);
        return data;
    }

    private WebClient resolvedClient() {
        WebClient client = cachedClient.get();
        if (client != null) {
            return client;
        }
        synchronized (cachedClient) {
            WebClient existing = cachedClient.get();
            if (existing != null) {
                return existing;
            }
            String baseUrl = requireNonBlank(properties.getBaseUrl(), "baseUrl");
            WebClient newClient = baseWebClient.mutate()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                .build();
            cachedClient.set(newClient);
            return newClient;
        }
    }

    private void ensureExists(Path pdfPath) {
        if (!Files.exists(pdfPath)) {
            throw new IllegalArgumentException("PDF file does not exist: " + pdfPath);
        }
        if (!Files.isRegularFile(pdfPath)) {
            throw new IllegalArgumentException("PDF path must be a regular file: " + pdfPath);
        }
    }

    private void notifyStatus(Consumer<String> statusCallback, String detail) {
        if (statusCallback != null && detail != null) {
            statusCallback.accept(detail);
        }
    }

    private void safeDelete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temporary file {}: {}", path, e.getMessage());
        }
    }

    private String buildAuthorizationHeader() {
        String token = requireNonBlank(properties.getToken(), "token");
        return "Bearer " + token;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Property " + name + " must be configured for Dolphin PDF service");
        }
        return value;
    }

    private <T> T block(Mono<T> mono, Duration timeout) {
        if (mono == null) {
            return null;
        }
        if (timeout != null) {
            return mono.block(timeout);
        }
        return mono.block();
    }

    private record TaskStatus(String status, String downloadUrl, String progress) { }
}
