package com.litevar.agent.rest.markdown_conversion.core;

import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;

public class MarkdownRenderer {
    public String render(Blocks.Document doc, boolean enableParagraphSplitting) {
        StringBuilder sb = new StringBuilder();
        if (doc.title != null && !doc.title.isBlank()) {
            sb.append("# ").append(doc.title.trim()).append("\n\n");
        }
        for (int i = 0; i < doc.blocks.size(); i++) {
            Blocks.Block b = doc.blocks.get(i);
            if (b instanceof Blocks.HorizontalRule) {
                sb.append("<!-- SPLITTING -->\n\n");
                continue;
            }
            if (b instanceof Blocks.Heading h) {
                sb.append("#".repeat(h.level)).append(" ").append(sanitize(h.text)).append("\n\n");
            } else if (b instanceof Blocks.Paragraph p) {
                sb.append(sanitize(p.text)).append("\n\n");
            } else if (b instanceof Blocks.Image im) {
                sb.append("![").append(sanitize(im.alt == null ? "" : im.alt)).append("](")
                        .append(im.path).append(")\n\n");
            } else if (b instanceof Blocks.Table t) {
                renderTable(sb, t);
            } else if (b instanceof Blocks.ListBlock lb) {
                renderList(sb, lb);
            }
            if (enableParagraphSplitting && i < doc.blocks.size() - 1 && shouldSplitAfterBlock(b)) {
                sb.append("<!-- SPLITTING -->\n\n");
            }
        }
        return sb.toString();
    }

    private boolean shouldSplitAfterBlock(Blocks.Block block) {
        return block instanceof Blocks.Paragraph
            || block instanceof Blocks.Heading
            || block instanceof Blocks.ListBlock
            || block instanceof Blocks.Table
            || block instanceof Blocks.Image;
    }

    private void renderTable(StringBuilder sb, Blocks.Table t) {
        // Determine if table can be in pure Markdown (no spans, no images)
        boolean simple = true;
        int colCount = t.rows.isEmpty() ? 0 : t.rows.get(0).cells.size();
        for (Blocks.Table.Row r : t.rows) {
            if (r.cells.size() != colCount) { simple = false; break; }
            for (Blocks.Table.Cell c : r.cells) {
                if (c.colspan > 1 || c.rowspan > 1 || !c.images.isEmpty() || c.skip) { simple = false; break; }
            }
            if (!simple) break;
        }

        if (simple) {
            // Markdown table
            if (t.rows.isEmpty()) return;
            Blocks.Table.Row header = t.rows.get(0);
            sb.append("| ");
            for (Blocks.Table.Cell c : header.cells) sb.append(sanitize(c.text)).append(" | ");
            sb.append("\n| ");
            for (int i = 0; i < header.cells.size(); i++) sb.append("--- | ");
            sb.append("\n");
            for (int i = 1; i < t.rows.size(); i++) {
                sb.append("| ");
                for (Blocks.Table.Cell c : t.rows.get(i).cells) sb.append(sanitize(c.text)).append(" | ");
                sb.append("\n");
            }
            sb.append("\n");
        } else {
            // HTML table fallback
            sb.append("<table>\n");
            boolean headerDone = false;
            for (int r = 0; r < t.rows.size(); r++) {
                Blocks.Table.Row row = t.rows.get(r);
                sb.append("  <tr>\n");
                for (Blocks.Table.Cell c : row.cells) {
                    if (c.skip) continue;
                    String tag = (!headerDone && t.hasHeader) ? "th" : "td";
                    sb.append("    <").append(tag);
                    if (c.colspan > 1) sb.append(" colspan=\"").append(c.colspan).append("\"");
                    if (c.rowspan > 1) sb.append(" rowspan=\"").append(c.rowspan).append("\"");
                    sb.append(">");
                    String txt = sanitize(c.text);
                    if (!txt.isEmpty()) sb.append(escapeHtml(txt));
                    for (Blocks.Image im : c.images) {
                        sb.append(txt.isEmpty()?"":"<br/>");
                        sb.append("<img src=\"").append(im.path).append("\" alt=\"")
                          .append(escapeHtml(im.alt == null?"":im.alt)).append("\"/>");
                    }
                    sb.append("</").append(tag).append(">\n");
                }
                sb.append("  </tr>\n");
                headerDone = true;
            }
            sb.append("</table>\n\n");
        }
    }

    private String sanitize(String s) { return s == null ? "" : s.replace("\r", "").trim(); }
    private String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private void renderList(StringBuilder sb, Blocks.ListBlock lb) {
        for (Blocks.ListBlock.Item it : lb.items) {
            int indent = Math.max(0, it.level) * 2;
            sb.append(" ".repeat(indent));
            sb.append(it.ordered ? "1. " : "- ");
            sb.append(sanitize(it.text)).append("\n");
        }
        sb.append("\n");
    }
}
