package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;
import com.litevar.agent.rest.util.IOUtil;
import org.apache.poi.hslf.usermodel.*;
import org.apache.poi.sl.usermodel.PictureData;

import java.nio.file.Files;
import java.nio.file.Path;

public class PptParser implements Parser {
    @Override
    public Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        Blocks.Document doc = new Blocks.Document();
        try (HSLFSlideShow ppt = new HSLFSlideShow(Files.newInputStream(file))) {
            int idx = 1;
            for (HSLFSlide slide : ppt.getSlides()) {
                String title = slide.getTitle();
                if (title != null && !title.isBlank()) doc.blocks.add(new Blocks.Heading(1, title));
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape ts) {
                        String text = ts.getText();
                        if (text != null && !text.isBlank()) doc.blocks.add(new Blocks.Paragraph(text));
                    } else if (shape instanceof HSLFPictureShape ps) {
                        PictureData pd = ps.getPictureData();
                        if (pd != null) {
                            byte[] data = pd.getData();
                            String ext = pd.getType().extension.replace(".", "");
                            String hash = IOUtil.sha1Hex(data);
                            String rel = String.format("assets/images/%s.%s", hash, ext);
                            doc.blocks.add(new Blocks.Image("slide-image", rel, data, ext));
                        }
                    }
                }
                if (idx++ < ppt.getSlides().size()) doc.blocks.add(new Blocks.HorizontalRule());
            }
        }
        return doc;
    }
}

