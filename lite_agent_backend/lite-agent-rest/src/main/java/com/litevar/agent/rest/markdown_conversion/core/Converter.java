package com.litevar.agent.rest.markdown_conversion.core;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.markdown_conversion.parser.*;
import com.litevar.agent.rest.util.IOUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
        String imagePrefix = DigestUtils.md5Hex(file.getFileName().toString());
        AtomicInteger imageCounter = new AtomicInteger(1);
        exportResources(doc, file, options, result, imagePrefix, imageCounter);

        MarkdownRenderer renderer = new MarkdownRenderer();
        boolean hasPageBreaks = doc.blocks.stream().anyMatch(b -> b instanceof Blocks.HorizontalRule);
        String lowerMime = type == null ? "" : type.toLowerCase();
        boolean isTextLike = lowerMime.startsWith("text/");
        boolean isWordDoc = lowerMime.contains("wordprocessingml") || lowerMime.contains("msword");
        boolean enableParagraphSplitting = !hasPageBreaks && (isTextLike || isWordDoc);
        String md = renderer.render(doc, enableParagraphSplitting);

        Path outDir = options.getOutputDir() != null ? options.getOutputDir() : file.getParent();
        String baseName = IOUtil.baseName(file);
        Path mdPath = outDir.resolve(baseName + ".md");
        Files.createDirectories(mdPath.getParent());
        notifyProgress(options, 0.85, STAGE_WRITING, "Writing markdown output");
        Files.writeString(mdPath, md, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        result.addMarkdown(mdPath);
        notifyProgress(options, 1.0, STAGE_COMPLETE, "Conversion completed");
    }

    private static void exportResources(Blocks.Document doc, Path file, ConversionOptions options, ConversionResult result,
                                        String imagePrefix, AtomicInteger imageCounter) throws IOException {
        Path outDir = options.getOutputDir() != null ? options.getOutputDir() : file.getParent();
        Path imageDir = outDir.resolve(options.getImageDir());
        //为了方便 nginx 代理，这里把图片路径改为绝对路径,再加上resources前缀
        String imageDirRel = Paths.get("/resources", StrUtil.removePrefix(imageDir.toString(), options.getPrefixPath())).toString();
        Files.createDirectories(imageDir);
        for (Blocks.Block b : doc.blocks) {
            if (b instanceof Blocks.Image im) {
                exportImage(im, imageDir, imageDirRel, result, imagePrefix, imageCounter);
            } else if (b instanceof Blocks.Table t) {
                for (Blocks.Table.Row row : t.rows) {
                    for (Blocks.Table.Cell cell : row.cells) {
                        for (Blocks.Image im : cell.images) {
                            exportImage(im, imageDir, imageDirRel, result, imagePrefix, imageCounter);
                        }
                    }
                }
            }
        }
    }

    private static void exportImage(Blocks.Image im, Path imageDir, String imageDirRel, ConversionResult result,
                                    String imagePrefix, AtomicInteger imageCounter) throws IOException {
        if (im.data != null && im.data.length > 0) {
            String ext = (im.extension == null || im.extension.isBlank()) ? "png" : im.extension.toLowerCase();
            int index = imageCounter.getAndIncrement();
            String fileName = imagePrefix + "_" + index + "." + ext;
            Path out = imageDir.resolve(fileName);
            Files.write(out, im.data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            im.path = imageDirRel + "/" + fileName;
            result.addResource(out);
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
