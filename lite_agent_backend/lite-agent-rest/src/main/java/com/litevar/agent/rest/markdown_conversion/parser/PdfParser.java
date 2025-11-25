package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.util.IOUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class PdfParser implements Parser {
    @Override
    public Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        Blocks.Document doc = new Blocks.Document();
        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
//            // Option to force OCR for PDFs
//            if (options != null && options.isForceOcr()) {
//                result.addWarning("forceOcr enabled; using OCR pipeline");
//                OcrPdfParser ocr = new OcrPdfParser();
//                return ocr.parse(file, options, result);
//            }
//            // Heuristic: if the PDF has near-zero extractable text on first pages,
//            // treat it as scanned and fall back to OCR-based parsing.
//            try {
//                if (isScannedPdf(pdf)) {
//                    result.addWarning("Detected scanned PDF; using OCR pipeline");
//                    OcrPdfParser ocr = new OcrPdfParser();
//                    return ocr.parse(file, options, result);
//                }
//            } catch (Throwable ignore) { /* best-effort detection only */ }
            ObjectExtractor extractor = null;
            try {
                extractor = new ObjectExtractor(pdf);
            } catch (Exception e) {
                result.addWarning("Tabula ObjectExtractor init failed: " + e.getMessage());
            }
            int pageIndex = 1;
            for (PDPage page : pdf.getPages()) {
                // 文本：每页独立提取并进行段落切分（空行/标点启发式），不做标题识别
                String pageText = extractPageText(pdf, pageIndex, result);
                for (String para : splitParagraphs(pageText)) {
                    if (!para.isBlank()) doc.blocks.add(new Blocks.Paragraph(para));
                }

                // 表格（Tabula）：在该页文本后插入（保守顺序）
                java.util.List<Blocks.Table> pageTables = new java.util.ArrayList<>();
                final float pageHeight = page.getCropBox() != null ? page.getCropBox().getHeight() : page.getMediaBox().getHeight();
                if (extractor != null && pageIndex > 1) { // 避免封面噪声表格，可后续做成可配置
                    try {
                        Page tPage = extractor.extract(pageIndex);
                        if (tPage != null) {
                            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                            BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
                            java.util.List<Table> tables = sea.isTabular(tPage) ? sea.extract(tPage) : bea.extract(tPage);
                            for (Table tb : tables) {
                                Blocks.Table bt = toBlocksTable(tb, pageHeight);
                                if (isMeaningfulTable(bt) && (pageIndex != 1 || isMeaningfulTableCover(bt))) {
                                    pageTables.add(bt);
                                }
                            }
                        }
                    } catch (Exception e) {
                        result.addWarning("Tabula table extraction failed on page " + pageIndex + ": " + e.getMessage());
                    }
                }
                for (Blocks.Table t : pageTables) doc.blocks.add(t);

                // 图片：紧随该页文本与表格后插入（遍历资源字典，稳定输出）
                try {
                    PDResources res = page.getResources();
                    if (res != null) {
                        Iterable<COSName> names = res.getXObjectNames();
                        if (names != null) {
                            for (COSName name : names) {
                                PDXObject xo = res.getXObject(name);
                                if (xo instanceof PDImageXObject img) {
                                    try {
                                        java.awt.image.BufferedImage bi = img.getImage();
                                        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                                        javax.imageio.ImageIO.write(bi, "png", bos);
                                        byte[] data = bos.toByteArray();
                                        String hash = IOUtil.sha1Hex(data);
                                        String rel = String.format("assets/images/%s.%s", hash, "png");
                                        doc.blocks.add(new Blocks.Image("image", rel, data, "png"));
                                    } catch (Exception ex) {
                                        // 单张图片异常不影响整页
                                        result.addWarning("Skip image due to error: " + ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    result.addWarning("Skip page images due to error: " + e.getMessage());
                }

                // 页间分隔
                doc.blocks.add(new Blocks.HorizontalRule());
                pageIndex++;
            }
            if (extractor != null) {
                try {
                    extractor.close();
                } catch (Exception ignore) {
                }
            }
        }
        return doc;
    }

    private static Blocks.Table toBlocksTable(Table tb, float pageHeight) {
        Blocks.Table t = new Blocks.Table();
        java.util.List<java.util.List<RectangularTextContainer>> rows = tb.getRows();
        for (java.util.List<RectangularTextContainer> r : rows) {
            Blocks.Table.Row br = new Blocks.Table.Row();
            for (RectangularTextContainer cell : r) {
                Blocks.Table.Cell bc = new Blocks.Table.Cell();
                String txt = cell.getText();
                if (txt != null) bc.text = txt.replace('\r', ' ').replace('\n', ' ').trim();
                try {
                    // Tabula 使用 top-left 原点；PDF 使用 bottom-left。转换到 PDF 坐标（bottom-left 原点）。
                    double left = cell.getLeft();
                    double topFromTop = cell.getTop();
                    double width = cell.getWidth();
                    double height = cell.getHeight();
                    double right = left + width;
                    double topPdf = pageHeight - topFromTop; // from bottom
                    double bottomPdf = topPdf - height;
                    bc.left = left;
                    bc.right = right;
                    bc.top = topPdf;
                    bc.bottom = bottomPdf;
                } catch (Throwable ignore) {
                }
                br.cells.add(bc);
            }
            t.rows.add(br);
        }
        t.hasHeader = true; // 默认首行为表头
        // 简单的横向合并推断：将右侧连续空单元格并入左侧有内容的单元格
        for (Blocks.Table.Row br : t.rows) {
            for (int c = 0; c < br.cells.size(); c++) {
                Blocks.Table.Cell cur = br.cells.get(c);
                if (cur.skip || cur.text.isBlank()) continue;
                int span = 1;
                int j = c + 1;
                while (j < br.cells.size() && br.cells.get(j).text.isBlank()) {
                    br.cells.get(j).skip = true;
                    span++;
                    j++;
                }
                cur.colspan = span;
            }
        }
        // 简单的纵向合并推断：将下方连续空单元格并入当前有内容的单元格
        int maxCols = 0;
        for (Blocks.Table.Row br : t.rows) maxCols = Math.max(maxCols, br.cells.size());
        for (int c = 0; c < maxCols; c++) {
            for (int r = 0; r < t.rows.size(); r++) {
                Blocks.Table.Cell cur = c < t.rows.get(r).cells.size() ? t.rows.get(r).cells.get(c) : null;
                if (cur == null || cur.skip || cur.text.isBlank()) continue;
                int span = 1;
                int rr = r + 1;
                while (rr < t.rows.size()) {
                    Blocks.Table.Cell below = c < t.rows.get(rr).cells.size() ? t.rows.get(rr).cells.get(c) : null;
                    if (below != null && below.text.isBlank()) {
                        below.skip = true;
                        span++;
                        rr++;
                    } else break;
                }
                cur.rowspan = span;
            }
        }
        return t;
    }

    private static boolean isMeaningfulTable(Blocks.Table t) {
        if (t.rows.isEmpty()) return false;
        int colCount = 0;
        for (Blocks.Table.Row r : t.rows) colCount = Math.max(colCount, r.cells.size());
        if (colCount < 2 || colCount > 12) return false;
        int total = 0, nonEmpty = 0, textLen = 0;
        int rows50 = 0;
        int rowsWith2 = 0;
        for (Blocks.Table.Row r : t.rows) {
            int rowNonEmpty = 0;
            int rowTotal = 0;
            for (Blocks.Table.Cell c : r.cells) {
                total++;
                rowTotal++;
                if (c.text != null && !c.text.isBlank()) {
                    nonEmpty++;
                    rowNonEmpty++;
                    textLen += c.text.length();
                }
            }
            if (rowTotal > 0 && (rowNonEmpty / (double) rowTotal) >= 0.5) rows50++;
            if (rowNonEmpty >= 2) rowsWith2++;
        }
        if (total == 0) return false;
        double fill = nonEmpty / (double) total;
        double avgNonEmptyPerRow = nonEmpty / (double) t.rows.size();
        double rowsWith2Ratio = rowsWith2 / (double) t.rows.size();
        // 更严格的过滤标准，尽量排除封面/排版性网格
        return fill >= 0.45 && textLen >= 60 && rowsWith2 >= 3 && rowsWith2Ratio >= 0.5 && avgNonEmptyPerRow >= 2.0;
    }

    // 对封面页更严格的过滤，尽量忽略排版性网格
    private static boolean isMeaningfulTableCover(Blocks.Table t) {
        if (t.rows.isEmpty()) return false;
        int colCount = 0;
        for (Blocks.Table.Row r : t.rows) colCount = Math.max(colCount, r.cells.size());
        if (colCount < 2 || colCount > 8) return false;
        int rowsWith2 = 0, textLen = 0;
        for (Blocks.Table.Row r : t.rows) {
            int rowNonEmpty = 0;
            for (Blocks.Table.Cell c : r.cells) {
                if (c.text != null && !c.text.isBlank()) {
                    rowNonEmpty++;
                    textLen += c.text.length();
                }
            }
            if (rowNonEmpty >= 2) rowsWith2++;
        }
        double ratio = rowsWith2 / (double) t.rows.size();
        return ratio >= 0.7 && rowsWith2 >= 4 && textLen >= 100;
    }

    private record ImageAt(double cx, double cy, double w, double h, byte[] data) {
        double left() {
            return cx - w / 2.0;
        }

        double right() {
            return cx + w / 2.0;
        }

        double bottom() {
            return cy - h / 2.0;
        }

        double top() {
            return cy + h / 2.0;
        }

        double area() {
            return Math.max(0, w) * Math.max(0, h);
        }
    }

    private static boolean pointInCell(double x, double y, Blocks.Table.Cell c) {
        double l = Math.min(c.left, c.right) - 2.0, r = Math.max(c.left, c.right) + 2.0;
        double b = Math.min(c.bottom, c.top) - 2.0, t = Math.max(c.bottom, c.top) + 2.0;
        return x >= l && x <= r && y >= b && y <= t;
    }

    private static boolean overlapsCell(ImageAt ia, Blocks.Table.Cell c) {
        double l1 = Math.min(c.left, c.right), r1 = Math.max(c.left, c.right);
        double b1 = Math.min(c.bottom, c.top), t1 = Math.max(c.bottom, c.top);
        double l2 = ia.left(), r2 = ia.right();
        double b2 = ia.bottom(), t2 = ia.top();
        double interW = Math.max(0, Math.min(r1, r2) - Math.max(l1, l2));
        double interH = Math.max(0, Math.min(t1, t2) - Math.max(b1, b2));
        double interA = interW * interH;
        if (interA <= 0) return false;
        double cellA = (r1 - l1) * (t1 - b1) + 1e-6;
        double imgA = ia.area() + 1e-6;
        double iou = interA / (cellA + imgA - interA);
        return iou >= 0.10 || (interA / cellA) >= 0.25 || (interA / imgA) >= 0.25 || pointInCell(ia.cx(), ia.cy(), c);
    }

    private static java.util.List<ImageAt> locateImagesOnPage(PDPage page) throws IOException {
        class Locator extends PDFStreamEngine {
            final java.util.List<ImageAt> images = new java.util.ArrayList<>();

            @Override
            protected void processOperator(Operator operator, java.util.List<org.apache.pdfbox.cos.COSBase> operands) throws IOException {
                String op = operator.getName();
                if ("Do".equals(op)) {
                    COSName name = (COSName) operands.get(0);
                    PDXObject xobject = getResources().getXObject(name);
                    if (xobject instanceof PDImageXObject img) {
                        try {
                            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                            float x = ctm.getTranslateX();
                            float y = ctm.getTranslateY();
                            float w = Math.abs(ctm.getScalingFactorX());
                            float h = Math.abs(ctm.getScalingFactorY());
                            double cx = x + w / 2.0, cy = y + h / 2.0;
                            java.awt.image.BufferedImage bi = img.getImage();
                            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                            javax.imageio.ImageIO.write(bi, "png", bos);
                            images.add(new ImageAt(cx, cy, w, h, bos.toByteArray()));
                        } catch (Exception ignore) {
                        }
                    } else if (xobject instanceof PDFormXObject form) {
                        showForm(form);
                    }
                } else {
                    super.processOperator(operator, operands);
                }
            }
        }
        Locator loc = new Locator();
        loc.processPage(page);
        // 过滤掉过小的图像，避免导出大量小图标/噪声
        java.util.ArrayList<ImageAt> filtered = new java.util.ArrayList<>();
        for (ImageAt ia : loc.images) {
            if (ia.w * ia.h >= 1600) { // 约 40x40 以上，过滤更小图标
                filtered.add(ia);
            }
        }
        return filtered;
    }

    private static java.util.List<String> splitParagraphs(String text) {
        java.util.ArrayList<String> paras = new java.util.ArrayList<>();
        if (text == null) return paras;
        String norm = text.replace("\r", "\n");
        String[] lines = norm.split("\n");
        StringBuilder buf = new StringBuilder();
        java.util.regex.Pattern bullet = java.util.regex.Pattern.compile("^\\s*([0-9]+[\\.)]|[\\u2022\\-\\*\\u00b7\\u2013\\u2014])\\s+.*$");
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                flush(buf, paras);
                continue;
            }
            if (bullet.matcher(line).matches()) {
                flush(buf, paras);
                String item = line.replaceFirst("^\\s*([0-9]+[\\.)]|[\\u2022\\-\\*\\u00b7\\u2013\\u2014])\\s+", "");
                paras.add("- " + item);
                continue;
            }
            buf.append(line);
            if (line.matches(".*([\\.!?])$") || line.matches(".*[。！？；：]$")) {
                flush(buf, paras);
            } else {
                buf.append(' ');
            }
        }
        flush(buf, paras);
        return paras;
    }

    private static void flush(StringBuilder buf, java.util.List<String> paras) {
        String s = buf.toString().trim();
        if (!s.isEmpty()) paras.add(s);
        buf.setLength(0);
    }

    // Detects scanned PDFs by sampling first pages and measuring extractable text amount
    // and presence of image content. If average non-whitespace chars per page is tiny while
    // images are present, we consider it scanned.
    private static boolean isScannedPdf(PDDocument pdf) throws Exception {
        int pages = pdf.getNumberOfPages();
        if (pages <= 0) return false;
        int sample = Math.min(5, pages);
        PDFTextStripper stripper = new PDFTextStripper();
        int totalChars = 0;
        int imgPages = 0;
        int largeImgPages = 0;
        for (int i = 1; i <= sample; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String text = stripper.getText(pdf);
            if (text != null) {
                totalChars += text.replaceAll("\\s+", "").length();
            }
            try {
                PDPage page = pdf.getPage(i - 1);
                java.util.List<ImageAt> imgs = locateImagesOnPage(page);
                if (imgs != null && !imgs.isEmpty()) {
                    imgPages++;
                    // compute largest image area ratio vs page area
                    double pageW = page.getCropBox() != null ? page.getCropBox().getWidth() : page.getMediaBox().getWidth();
                    double pageH = page.getCropBox() != null ? page.getCropBox().getHeight() : page.getMediaBox().getHeight();
                    double pageArea = Math.max(1.0, pageW * pageH);
                    double maxImgArea = 0;
                    for (ImageAt ia : imgs) {
                        double a = ia.w * ia.h;
                        if (a > maxImgArea) maxImgArea = a;
                    }
                    double ratio = maxImgArea / pageArea;
                    if (ratio >= 0.5) largeImgPages++;
                }
            } catch (Throwable ignore) {
            }
        }
        double avgChars = totalChars / (double) sample;
        double imgRatio = imgPages / (double) sample;
        double largeImgRatio = largeImgPages / (double) sample;
        // Consider scanned if: low text and image-heavy, or many pages dominated by a large image
        return (avgChars < 200.0 && imgRatio >= 0.4) || largeImgRatio >= 0.6;
    }

    private static String extractPageText(PDDocument pdf, int pageIndex, ConversionResult result) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        try {
            return stripper.getText(pdf);
        } catch (IOException e) {
            if (isRecoverableFontParseError(e)) {
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (result != null) {
                    result.addWarning("Skip text extraction on page " + pageIndex + ": " + reason);
                }
                return "";
            }
            throw e;
        }
    }

    private static boolean isRecoverableFontParseError(IOException e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof IOException io) {
                String msg = io.getMessage();
                if (msg != null) {
                    String lower = msg.toLowerCase(Locale.ROOT);
                    if (lower.contains("table is mandatory") || lower.contains("true type fonts using cff outlines")) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
