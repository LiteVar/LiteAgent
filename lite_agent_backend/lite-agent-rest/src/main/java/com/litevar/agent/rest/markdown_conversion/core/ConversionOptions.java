package com.litevar.agent.rest.markdown_conversion.core;

import lombok.Getter;

import java.nio.file.Path;

@Getter
public class ConversionOptions {
    private final Path outputDir;
    private final String imageDir;
    private final ConversionProgressListener progressListener;
    private final String prefixPath;

    private ConversionOptions(Builder b) {
        this.outputDir = b.outputDir;
        this.imageDir = b.imageDir == null ? "assets/images" : b.imageDir;
        this.progressListener = b.progressListener;
        this.prefixPath = b.prefixPath;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Path outputDir;
        private String imageDir;
        private ConversionProgressListener progressListener;
        private String prefixPath;

        public Builder prefixPath(String prefixPath) { this.prefixPath = prefixPath; return this; }
        public Builder outputDir(Path outputDir) { this.outputDir = outputDir; return this; }
        public Builder imageDir(String imageDir) { this.imageDir = imageDir; return this; }
        public Builder progressListener(ConversionProgressListener listener) { this.progressListener = listener; return this; }
        public ConversionOptions build() { return new ConversionOptions(this); }
    }
}
