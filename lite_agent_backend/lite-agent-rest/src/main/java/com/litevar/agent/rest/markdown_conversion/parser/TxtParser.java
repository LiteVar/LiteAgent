package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TxtParser implements Parser {
    @Override
    public Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception {
        Blocks.Document doc = new Blocks.Document();
        Charset cs = StandardCharsets.UTF_8; // TODO: detect BOM/charset if needed
        List<String> lines = Files.readAllLines(file, cs);
        List<String> buf = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                flush(buf, doc);
            } else {
                buf.add(line);
            }
        }
        flush(buf, doc);
        return doc;
    }

    private static void flush(List<String> buf, Blocks.Document doc) {
        if (buf.isEmpty()) return;
        String text = String.join(" ", buf).trim();
        if (text.startsWith("#")) {
            int level = 0; while (level < text.length() && text.charAt(level) == '#') level++;
            String t = text.substring(level).trim();
            doc.blocks.add(new Blocks.Heading(Math.max(1, Math.min(6, level)), t));
        } else {
            doc.blocks.add(new Blocks.Paragraph(text));
        }
        buf.clear();
    }
}

