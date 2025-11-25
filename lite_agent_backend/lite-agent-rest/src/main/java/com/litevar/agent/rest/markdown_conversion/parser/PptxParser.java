package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.util.IOUtil;
import org.apache.poi.xslf.usermodel.*;

import java.nio.file.Path;

public class PptxParser implements Parser {
    @Override
    public Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        Blocks.Document doc = new Blocks.Document();
        try (XMLSlideShow ppt = new XMLSlideShow(java.nio.file.Files.newInputStream(file))) {
            int idx = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                String title = slide.getTitle();
                if (title != null && !title.isBlank()) doc.blocks.add(new Blocks.Heading(1, title));
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape ts) {
                        for (XSLFTextParagraph para : ts.getTextParagraphs()) {
                            String text = para.getText();
                            if (text != null && !text.isBlank()) doc.blocks.add(new Blocks.Paragraph(text));
                        }
                    } else if (shape instanceof XSLFPictureShape ps) {
                        XSLFPictureData pd = ps.getPictureData();
                        if (pd != null) {
                            byte[] data = pd.getData();
                            String ext = pd.suggestFileExtension();
                            String hash = IOUtil.sha1Hex(data);
                            String rel = String.format("assets/images/%s.%s", hash, ext);
                            doc.blocks.add(new Blocks.Image("slide-image", rel, data, ext));
                        }
                    } else if (shape instanceof XSLFTable tbl) {
                        Blocks.Table table = new Blocks.Table();
                        for (XSLFTableRow tr : tbl.getRows()) {
                            Blocks.Table.Row br = new Blocks.Table.Row();
                            for (XSLFTableCell tc : tr.getCells()) {
                                Blocks.Table.Cell bc = new Blocks.Table.Cell();
                                String t = tc.getText();
                                if (t != null) bc.text = t.trim();
                                // TODO: colspan/rowspan if API available; default 1
                                br.cells.add(bc);
                            }
                            table.rows.add(br);
                        }
                        doc.blocks.add(table);
                    }
                }
                if (idx++ < ppt.getSlides().size()) doc.blocks.add(new Blocks.HorizontalRule());
            }
        }
        return doc;
    }
}
