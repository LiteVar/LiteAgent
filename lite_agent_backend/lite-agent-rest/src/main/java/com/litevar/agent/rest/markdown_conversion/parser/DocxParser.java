package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.util.IOUtil;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocxParser implements Parser {
    @Override
    public Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        Blocks.Document doc = new Blocks.Document();
        try (OPCPackage pkg = OPCPackage.open(file.toFile()); XWPFDocument xdoc = new XWPFDocument(pkg)) {
            Set<String> captured = new HashSet<>();
            if (xdoc.getProperties() != null && xdoc.getProperties().getCoreProperties() != null) {
                String title = xdoc.getProperties().getCoreProperties().getTitle();
                doc.title = title;
            }
            for (IBodyElement e : xdoc.getBodyElements()) {
                if (e instanceof XWPFParagraph p) {
                    handleParagraph(doc, p, captured);
                } else if (e instanceof XWPFTable t) {
                    handleTable(doc, t, captured);
                }
            }

            // Pictures not already captured in runs (fallback)
            List<XWPFPictureData> pics = xdoc.getAllPictures();
            for (XWPFPictureData data : pics) {
                var image = toImage(data);
                if (image == null) continue;
                if (captured.contains(image.hash())) continue;
                captured.add(image.hash());
                doc.blocks.add(image.image());
            }
        }
        return doc;
    }

    private void handleParagraph(Blocks.Document doc, XWPFParagraph p, Set<String> captured) {
        String style = p.getStyle();
        String text = p.getText();
        if (text == null) text = "";
        int headingLevel = headingLevel(style);
        if (headingLevel > 0) {
            doc.blocks.add(new Blocks.Heading(headingLevel, text));
            return;
        }
        // 简单列表识别：存在编号ID则作为列表项输出
        if (p.getNumID() != null) {
            String t = text.strip();
            if (!t.isEmpty()) doc.blocks.add(new Blocks.Paragraph("- " + t));
        }
        // Images embedded in runs
        for (IRunElement r : p.getIRuns()) {
            if (r instanceof XWPFRun run) {
                for (XWPFPicture pic : run.getEmbeddedPictures()) {
                    var data = pic.getPictureData();
                    if (data == null) continue;
                    var image = toImage(data);
                    if (image == null) continue;
                    captured.add(image.hash());
                    doc.blocks.add(image.image());
                }
            }
        }
        if (!text.isBlank() && p.getNumID() == null) doc.blocks.add(new Blocks.Paragraph(text));
    }

    private int headingLevel(String style) {
        if (style == null) return 0;
        String s = style.toLowerCase();
        if (s.contains("heading 1") || s.equals("heading1")) return 1;
        if (s.contains("heading 2") || s.equals("heading2")) return 2;
        if (s.contains("heading 3") || s.equals("heading3")) return 3;
        if (s.contains("heading 4") || s.equals("heading4")) return 4;
        if (s.contains("heading 5") || s.equals("heading5")) return 5;
        if (s.contains("heading 6") || s.equals("heading6")) return 6;
        return 0;
    }

    private void handleTable(Blocks.Document doc, XWPFTable t, Set<String> captured) {
        Blocks.Table table = new Blocks.Table();
        var rows = t.getRows();
        int rowCount = rows.size();
        // First pass build cells with text/images and colspans
        for (int r = 0; r < rowCount; r++) {
            XWPFTableRow row = rows.get(r);
            Blocks.Table.Row br = new Blocks.Table.Row();
            List<XWPFTableCell> cells = row.getTableCells();
            for (int c = 0; c < cells.size(); c++) {
                XWPFTableCell xc = cells.get(c);
                Blocks.Table.Cell bc = new Blocks.Table.Cell();
                // text
                StringBuilder txt = new StringBuilder();
                for (XWPFParagraph p : xc.getParagraphs()) {
                    if (!p.getText().isBlank()) {
                        if (txt.length() > 0) txt.append(" \n");
                        txt.append(p.getText().trim());
                    }
                    // images embedded in runs
                    for (IRunElement re : p.getIRuns()) {
                        if (re instanceof XWPFRun run) {
                            for (XWPFPicture pic : run.getEmbeddedPictures()) {
                                var data = pic.getPictureData();
                                if (data == null) continue;
                                var image = toImage(data);
                                if (image == null) continue;
                                captured.add(image.hash());
                                bc.images.add(image.image());
                            }
                        }
                    }
                }
                bc.text = txt.toString();
                // colspan
                var tcPr = xc.getCTTc().getTcPr();
                if (tcPr != null && tcPr.getGridSpan() != null && tcPr.getGridSpan().getVal() != null) {
                    bc.colspan = tcPr.getGridSpan().getVal().intValue();
                }
                // rowspan (vMerge): handled in second pass by counting continues
                br.cells.add(bc);
            }
            table.rows.add(br);
        }
        // Second pass handle vertical merge (rowspan)
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            List<XWPFTableCell> cells = row.getTableCells();
            for (int c = 0; c < cells.size(); c++) {
                XWPFTableCell xc = cells.get(c);
                var tcPr = xc.getCTTc().getTcPr();
                if (tcPr != null && tcPr.getVMerge() != null) {
                    String val = tcPr.getVMerge().getVal() == null ? "continue" : tcPr.getVMerge().getVal().toString();
                    if ("restart".equalsIgnoreCase(val)) {
                        int span = 1;
                        for (int rr = r + 1; rr < rows.size(); rr++) {
                            List<XWPFTableCell> cellsBelow = rows.get(rr).getTableCells();
                            if (c >= cellsBelow.size()) break;
                            var tcPrBelow = cellsBelow.get(c).getCTTc().getTcPr();
                            if (tcPrBelow != null && tcPrBelow.getVMerge() != null) {
                                String v2 = tcPrBelow.getVMerge().getVal() == null ? "continue" : tcPrBelow.getVMerge().getVal().toString();
                                if ("continue".equalsIgnoreCase(v2)) {
                                    span++;
                                    // mark the cell as skipped
                                    table.rows.get(rr).cells.get(c).skip = true;
                                } else break;
                            } else break;
                        }
                        table.rows.get(r).cells.get(c).rowspan = span;
                    } else if ("continue".equalsIgnoreCase(val)) {
                        table.rows.get(r).cells.get(c).skip = true;
                    }
                }
            }
        }
        doc.blocks.add(table);
    }

    private ImageResult toImage(XWPFPictureData data) {
        if (data == null) return null;
        byte[] bytes = data.getData();
        if (bytes == null || bytes.length == 0) return null;
        String fileName = data.getFileName();
        if (fileName == null || fileName.isBlank()) {
            String extSuggestion = data.suggestFileExtension();
            if (extSuggestion == null || extSuggestion.isBlank()) extSuggestion = "png";
            fileName = "image." + extSuggestion;
        }
        String ext = IOUtil.extension(fileName);
        if (ext.isBlank()) {
            String fallback = data.suggestFileExtension();
            if (fallback != null && !fallback.isBlank()) ext = fallback;
        }
        ext = ext.isBlank() ? "png" : ext.toLowerCase();
        String hash = IOUtil.sha1Hex(bytes);
        String rel = String.format("assets/images/%s.%s", hash, ext);
        return new ImageResult(new Blocks.Image(fileName, rel, bytes, ext), hash);
    }

    private record ImageResult(Blocks.Image image, String hash) { }
}
