package com.litevar.agent.rest.markdown_conversion.core;

import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.markdown_conversion.parser.*;
import com.litevar.agent.rest.service.UploadFileServiceV2;
import com.litevar.agent.rest.util.IOUtil;
import com.litevar.agent.base.util.SpringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class Converter {
    private static final Tika TIKA = new Tika();

    private static final String STAGE_START = "START";
    private static final String STAGE_DETECT_TYPE = "DETECT_TYPE";
    private static final String STAGE_SELECT_PARSER = "SELECT_PARSER";
    private static final String STAGE_PARSING = "PARSING";
    private static final String STAGE_EXPORTING = "EXPORTING_RESOURCES";
    private static final String STAGE_WRITING = "WRITING_MARKDOWN";
    private static final String STAGE_COMPLETE = "COMPLETE";
    private static final String STAGE_UNSUPPORTED = "UNSUPPORTED";

    public static ConversionResult convert(Path inputPath, ConversionOptions options) throws Exception {
        if (Files.isDirectory(inputPath)) {
            throw new IllegalArgumentException("Converter only supports single file input");
        }
        ConversionResult result = new ConversionResult();
        notifyProgress(options, 0.0, STAGE_START, "Preparing conversion");
        convertSingle(inputPath, options, result);
        return result;
    }

    private static void convertSingle(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        notifyProgress(options, 0.05, STAGE_DETECT_TYPE, "Detecting file type");
        String type = detect(file);
        notifyProgress(options, 0.1, STAGE_SELECT_PARSER, "Selecting parser for " + type);
        Parser parser = selectParser(type, file);
        if (parser == null) {
            result.addWarning("No parser for: " + file + " (" + type + ")");
            notifyProgress(options, 1.0, STAGE_UNSUPPORTED, "No parser available for input");
            return;
        }

        notifyProgress(options, 0.2, STAGE_PARSING, "Parsing input document");
        Blocks.Document doc = parser.parse(file, options, result);
        notifyProgress(options, 0.6, STAGE_PARSING, "Finished parsing document");
        // export resources (images)
        notifyProgress(options, 0.7, STAGE_EXPORTING, "Exporting embedded resources");
        String imagePrefix = FilenameUtils.getBaseName(file.getFileName().toString());
        AtomicInteger imageCounter = new AtomicInteger(1);
        exportResources(doc, file, options, result, imagePrefix, imageCounter);

        MarkdownRenderer renderer = new MarkdownRenderer();
        boolean hasPageBreaks = doc.blocks.stream().anyMatch(b -> b instanceof Blocks.HorizontalRule);
        String lowerMime = type == null ? "" : type.toLowerCase();
        boolean isTextLike = lowerMime.startsWith("text/");
        boolean isWordDoc = lowerMime.contains("wordprocessingml") || lowerMime.contains("msword");
        boolean enableParagraphSplitting = !hasPageBreaks && (isTextLike || isWordDoc);
        String mdContent = renderer.render(doc, enableParagraphSplitting);

        String markdownName = IOUtil.baseName(file) + ".md";
        Path markdownDir = options.getOutputDir().resolve(markdownName);

        notifyProgress(options, 0.85, STAGE_WRITING, "Writing markdown output");

        SpringUtil.getBean(UploadFileServiceV2.class).saveConvertedMarkdown(options.getFileId(), markdownDir, mdContent);
        result.addMarkdown(markdownDir);
        notifyProgress(options, 1.0, STAGE_COMPLETE, "Conversion completed");
    }

    private static void exportResources(Blocks.Document doc, Path file, ConversionOptions options, ConversionResult result,
                                        String imagePrefix, AtomicInteger imageCounter) throws IOException {
        Path imageDir = options.getOutputDir().resolve(options.getImageDir());
        Files.createDirectories(imageDir);

        for (Blocks.Block b : doc.blocks) {
            if (b instanceof Blocks.Image im) {
                exportImage(im, imageDir, options, result, imagePrefix, imageCounter);
            } else if (b instanceof Blocks.Table t) {
                for (Blocks.Table.Row row : t.rows) {
                    for (Blocks.Table.Cell cell : row.cells) {
                        for (Blocks.Image im : cell.images) {
                            exportImage(im, imageDir, options, result, imagePrefix, imageCounter);
                        }
                    }
                }
            }
        }
    }

    private static void exportImage(Blocks.Image im, Path imageDir, ConversionOptions options, ConversionResult result,
                                    String imagePrefix, AtomicInteger imageCounter) throws IOException {
        if (im.data != null && im.data.length > 0) {
            String ext = (im.extension == null || im.extension.isBlank()) ? "png" : im.extension.toLowerCase();
            int index = imageCounter.getAndIncrement();
            String fileName = imagePrefix + "_" + index + "." + ext;
            Path path = imageDir.resolve(fileName);

            SpringUtil.getBean(UploadFileServiceV2.class)
                .saveMarkdownImage(im.data, fileName, imageDir.toString());

            im.path = "./imgs/" + fileName;
            result.addResource(path);
        }
    }

    private static String detect(Path file) throws IOException {
        try {
            return TIKA.detect(file);
        } catch (Throwable e) {
            return guessByExt(file);
        }
    }

    private static String guessByExt(Path file) {
        String n = file.getFileName().toString().toLowerCase();
        if (n.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (n.endsWith(".doc")) return "application/msword";
        if (n.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (n.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private static Parser selectParser(String mime, Path file) {
        if (mime == null) return null;
        mime = mime.toLowerCase();
        if (mime.contains("wordprocessingml.document") || file.toString().toLowerCase().endsWith(".docx")) return new DocxParser();
        if (mime.contains("msword") && file.toString().toLowerCase().endsWith(".doc")) return new DocParser();
        if (mime.contains("presentationml.presentation") || file.toString().toLowerCase().endsWith(".pptx")) return new PptxParser();
        if (mime.contains("ms-powerpoint") && file.toString().toLowerCase().endsWith(".ppt")) return new PptParser();
        if (mime.contains("pdf")) return new PdfParser();
        if (mime.startsWith("text/")) return new TxtParser();
        return null;
    }

    private static void notifyProgress(ConversionOptions options, double progress, String stage, String detail) {
        if (options == null) {
            return;
        }
        ConversionProgressListener listener = options.getProgressListener();
        if (listener == null) {
            return;
        }
        double normalized = Math.max(0d, Math.min(1d, progress));
        listener.onProgress(normalized, stage, detail);
    }
}
