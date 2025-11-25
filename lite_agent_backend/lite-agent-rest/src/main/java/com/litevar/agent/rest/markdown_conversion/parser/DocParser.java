package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.util.IOUtil;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DocParser implements Parser {
    @Override
    public Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        Blocks.Document doc = new Blocks.Document();
        try (HWPFDocument hwpf = new HWPFDocument(Files.newInputStream(file))) {
            Range range = hwpf.getRange();
            PicturesTable pictures = hwpf.getPicturesTable();

            // Collect tables in document order
            List<Table> tables = new ArrayList<>();
            TableIterator ti = new TableIterator(range);
            while (ti.hasNext()) tables.add(ti.next());

            int paraIndex = 0;
            int tableIndex = 0;
            while (paraIndex < range.numParagraphs()) {
                Paragraph p = range.getParagraph(paraIndex);

                // If next table starts before/at this paragraph and contains it, emit the table once
                if (tableIndex < tables.size()) {
                    Table t = tables.get(tableIndex);
                    int pStart = p.getStartOffset();
                    if (pStart >= t.getStartOffset() && pStart < t.getEndOffset()) {
                        doc.blocks.add(convertTable(t, pictures));
                        // Skip paragraphs covered by this table
                        paraIndex += t.numParagraphs();
                        tableIndex++;
                        continue;
                    }
                }

                // Regular paragraph outside tables
                handleParagraph(doc, p, pictures);
                paraIndex++;
            }
        }
        result.addWarning("DOC parsing: basic tables extracted; styles may be lost: " + file.getFileName());
        return doc;
    }

    private void handleParagraph(Blocks.Document doc, Paragraph paragraph, PicturesTable pictures) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < paragraph.numCharacterRuns(); i++) {
            CharacterRun run = paragraph.getCharacterRun(i);
            if (pictures.hasPicture(run)) {
                Blocks.Image image = toImage(pictures.extractPicture(run, false));
                if (image != null) doc.blocks.add(image);
                continue;
            }
            String runText = run.text();
            if (runText != null) text.append(runText);
        }
        String sanitized = sanitizeParagraphText(text.toString());
        if (!sanitized.isEmpty()) doc.blocks.add(new Blocks.Paragraph(sanitized));
    }

    private Blocks.Table convertTable(Table t, PicturesTable pictures) {
        Blocks.Table table = new Blocks.Table();
        for (int r = 0; r < t.numRows(); r++) {
            TableRow tr = t.getRow(r);
            Blocks.Table.Row br = new Blocks.Table.Row();
            for (int c = 0; c < tr.numCells(); c++) {
                TableCell tc = tr.getCell(c);
                Blocks.Table.Cell bc = new Blocks.Table.Cell();
                fillCell(tc, bc, pictures);
                br.cells.add(bc);
            }
            table.rows.add(br);
        }
        return table;
    }

    private void fillCell(TableCell tc, Blocks.Table.Cell target, PicturesTable pictures) {
        StringBuilder cellText = new StringBuilder();
        for (int pi = 0; pi < tc.numParagraphs(); pi++) {
            Paragraph paragraph = tc.getParagraph(pi);
            StringBuilder para = new StringBuilder();
            for (int ri = 0; ri < paragraph.numCharacterRuns(); ri++) {
                CharacterRun run = paragraph.getCharacterRun(ri);
                if (pictures.hasPicture(run)) {
                    Blocks.Image image = toImage(pictures.extractPicture(run, false));
                    if (image != null) target.images.add(image);
                    continue;
                }
                String runText = run.text();
                if (runText != null) para.append(runText);
            }
            String sanitized = sanitizeParagraphText(para.toString());
            if (!sanitized.isEmpty()) {
                if (cellText.length() > 0) cellText.append(" \n");
                cellText.append(sanitized);
            }
        }
        target.text = cellText.toString();
    }

    private String sanitizeParagraphText(String raw) {
        if (raw == null) return "";
        // Remove control chars commonly found in DOC (e.g., cell/end markers \u0007)
        String s = raw.replace('\r', ' ').replace('\n', ' ')
                .replace("\u0007", " ")
                .replace("\u000b", " ");
        return s.trim();
    }

    private Blocks.Image toImage(Picture picture) {
        if (picture == null) return null;
        byte[] data = picture.getContent();
        if (data == null || data.length == 0) return null;
        String fileName = picture.suggestFullFileName();
        if (fileName == null || fileName.isBlank()) {
            String ext = picture.suggestFileExtension();
            if (ext == null || ext.isBlank()) ext = "png";
            fileName = "image." + ext;
        }
        String ext = IOUtil.extension(fileName);
        if (ext.isBlank()) {
            String fallback = picture.suggestFileExtension();
            if (fallback != null && !fallback.isBlank()) ext = fallback;
        }
        ext = ext.isBlank() ? "png" : ext.toLowerCase();
        String hash = IOUtil.sha1Hex(data);
        String rel = String.format("assets/images/%s.%s", hash, ext);
        return new Blocks.Image(fileName, rel, data, ext);
    }
}
