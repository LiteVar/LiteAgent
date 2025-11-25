package com.litevar.agent.rest.markdown_conversion.core;

import lombok.Getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ConversionResult {
    private final List<Path> markdownFiles = new ArrayList<>();
    private final List<Path> exportedResources = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addMarkdown(Path p) { if (p != null) markdownFiles.add(p); }
    public void addResource(Path p) { if (p != null) exportedResources.add(p); }
    public void addWarning(String w) { if (w != null && !w.isBlank()) warnings.add(w); }

    public String getSummary() {
        return String.format("%d markdown file(s), %d resource(s), %d warning(s)",
                markdownFiles.size(), exportedResources.size(), warnings.size());
    }
}

