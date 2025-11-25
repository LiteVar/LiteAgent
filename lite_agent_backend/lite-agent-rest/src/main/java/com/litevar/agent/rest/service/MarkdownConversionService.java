package com.litevar.agent.rest.service;

import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.MarkdownConversionProgressDTO;
import com.litevar.agent.base.entity.UploadFile;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.Converter;
import com.litevar.agent.rest.markdown_conversion.detector.PdfScanDetector;
import com.litevar.agent.rest.markdown_conversion.dolphin.DolphinPdfMarkdownClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class MarkdownConversionService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    private static final long PROGRESS_TTL_HOURS = 1L;

    @Autowired
    private UploadFileService uploadFileService;

    @Autowired
    private PdfScanDetector pdfScanDetector;

    @Autowired
    private DolphinPdfMarkdownClient dolphinPdfMarkdownClient;
    @Autowired
    private LitevarProperties litevarProperties;

    @Async("asyncTaskExecutor")
    public void convertToMarkdownAsync(UploadFile uf) {
        try {
            reportProgress(uf, 0, "START", "Starting conversion", STATUS_RUNNING);
            Path source = Path.of(uf.getPath());
            Path output = Path.of(source.getParent().toString(), "md");
            Files.createDirectories(output);

            if (isPdf(source) && tryScannedPdfPipeline(uf, source, output)) {
                return;
            }

            Path datasets = Paths.get(litevarProperties.getUploadPath(), "datasets");

            ConversionOptions options = ConversionOptions.builder()
                .outputDir(output)
                .prefixPath(datasets.toString())
                .imageDir("imgs")
                .progressListener((progress, stage, detail) ->
                    reportProgress(uf, progress * 100, stage, detail, STATUS_RUNNING))
                .build();

            ConversionResult result = Converter.convert(source, options);
            boolean hasMarkdown = !result.getMarkdownFiles().isEmpty();
            if (hasMarkdown) {
                Path markdownFile = result.getMarkdownFiles().get(0);
                persistMarkdownMetadata(uf, markdownFile, source);
            } else {
                log.warn("Markdown conversion produced no markdown output for fileId={}, source={}", uf.getId(), source);
            }
            log.info("Markdown conversion finished for fileId={}, source={}, outputDir={}, summary={}",
                uf.getId(), source, output, result.getSummary());
            String finalStage = hasMarkdown ? "COMPLETE" : "UNSUPPORTED";
            String finalDetail = hasMarkdown ? "Conversion completed" : "No markdown output generated";
            reportProgress(uf, 100, finalStage, finalDetail, STATUS_COMPLETED);
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            reportProgress(uf, 100, "FAILED", detail, STATUS_FAILED);
            log.error("Markdown conversion failed for fileId={}", uf.getId(), e);
        }
    }

    private void reportProgress(UploadFile uf, double progress, String stage, String detail, String status) {
        if (uf == null || uf.getId() == null) {
            return;
        }
        double clamped = Math.max(0d, Math.min(100d, progress));
        String detailValue = detail == null ? "" : detail;
        MarkdownConversionProgressDTO payload = new MarkdownConversionProgressDTO(
            uf.getId(),
            clamped,
            stage,
            detailValue,
            status
        );
        String key = String.format(CacheKey.MARKDOWN_CONVERSION_PROGRESS, uf.getId());
        RedisUtil.setValue(key, payload, PROGRESS_TTL_HOURS, TimeUnit.HOURS);
    }

    private boolean tryScannedPdfPipeline(UploadFile uf, Path source, Path output) throws Exception {
        boolean scanned = detectScannedPdf(source);
        if (!scanned) {
            return false;
        }
        reportProgress(uf, 10, "SCANNED_PDF_DETECTED", "Detected scanned PDF, using Dolphin OCR pipeline", STATUS_RUNNING);
        AtomicInteger progressCounter = new AtomicInteger(25);
        Path markdownFile = dolphinPdfMarkdownClient.convert(source, output, detail -> {
            int current = progressCounter.get();
            reportProgress(uf, current, "SCANNED_PDF_CONVERSION", detail, STATUS_RUNNING);
            int next = Math.min(90, current + 10);
            progressCounter.set(next);
        });
        replaceDolphinSeparators(markdownFile);
        persistMarkdownMetadata(uf, markdownFile, source);
        log.info("Scanned PDF conversion completed for fileId={}, source={}, markdown={}",
            uf.getId(), source, markdownFile);
        reportProgress(uf, 100, "COMPLETE", "Conversion completed", STATUS_COMPLETED);
        return true;
    }

    private boolean detectScannedPdf(Path source) {
        try {
            boolean scanned = pdfScanDetector.isLikelyScanned(source);
            log.info("Scanned PDF detection for source={} result={}", source, scanned);
            return scanned;
        } catch (IOException e) {
            log.warn("Failed to detect scanned PDF for source={}, fallback to native parser: {}", source, e.getMessage());
            return false;
        }
    }

    private void persistMarkdownMetadata(UploadFile uf, Path markdownFile, Path source) {
        if (markdownFile == null) {
            return;
        }
        String markdownName = markdownFile.getFileName().toString();
        String markdownPath = markdownFile.toString();
        uf.setMarkdownName(markdownName);
        uf.setMarkdownPath(markdownPath);
        if (uf.getId() != null) {
            uploadFileService.update(
                uploadFileService.lambdaUpdate()
                    .set(UploadFile::getMarkdownName, markdownName)
                    .set(UploadFile::getMarkdownPath, markdownPath)
                    .eq(UploadFile::getId, uf.getId())
            );
        } else {
            log.warn("UploadFile id is null, skip persisting markdown metadata for source={}", source);
        }
    }

    private boolean isPdf(Path source) {
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf");
    }

    private void replaceDolphinSeparators(Path markdownFile) {
        if (markdownFile == null) {
            return;
        }
        try {
            String content = Files.readString(markdownFile, StandardCharsets.UTF_8);
            String updated = content.replaceAll("(?m)^\\s*---\\s*$", "<!-- SPLITTING -->");
            if (!content.equals(updated)) {
                Files.writeString(markdownFile, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to normalize page separators for Dolphin markdown {}: {}", markdownFile, e.getMessage());
        }
    }
}
