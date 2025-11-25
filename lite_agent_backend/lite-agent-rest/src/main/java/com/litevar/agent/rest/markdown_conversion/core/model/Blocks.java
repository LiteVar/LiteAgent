package com.litevar.agent.rest.markdown_conversion.core.model;

import java.util.ArrayList;
import java.util.List;

public class Blocks {
    public static class Document {
        public final List<Block> blocks = new ArrayList<>();
        public String title;
    }

    public static abstract class Block { }

    public static class Heading extends Block {
        public final int level; public final String text;
        public Heading(int level, String text) { this.level = Math.max(1, Math.min(6, level)); this.text = text; }
    }
    public static class Paragraph extends Block {
        public final String text; public Paragraph(String t) { this.text = t; }
    }
    public static class HorizontalRule extends Block { }

    public static class Image extends Block {
        public final String alt;
        public String path; // relative path after export
        public final byte[] data; // original bytes if available
        public final String extension;
        public Image(String alt, String path, byte[] data, String extension) {
            this.alt = alt; this.path = path; this.data = data; this.extension = extension;
        }
    }

    public static class Table extends Block {
        public final List<Row> rows = new ArrayList<>();
        public boolean hasHeader = true;
        public static class Row {
            public final List<Cell> cells = new ArrayList<>();
        }
        public static class Cell {
            public String text = "";
            public final List<Image> images = new ArrayList<>();
            public int colspan = 1;
            public int rowspan = 1;
            public boolean skip = false; // used for merged-into cells
            // PDF cell bounds (points) for mapping images
            public double left, top, right, bottom;
        }
    }

    public static class ListBlock extends Block {
        public final List<Item> items = new ArrayList<>();
        public static class Item {
            public final int level; // 0-based
            public final boolean ordered;
            public final String text;
            public Item(int level, boolean ordered, String text) {
                this.level = Math.max(0, level); this.ordered = ordered; this.text = text;
            }
        }
    }
}
