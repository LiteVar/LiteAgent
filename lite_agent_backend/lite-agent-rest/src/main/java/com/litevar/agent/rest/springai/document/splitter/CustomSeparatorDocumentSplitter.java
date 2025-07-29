package com.litevar.agent.rest.springai.document.splitter;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author reid
 * @since 2025/6/16
 */

public class CustomSeparatorDocumentSplitter extends AbstractDocumentSplitter {

    private final String[] separators;
    private final boolean useRegex;

    public CustomSeparatorDocumentSplitter(int maxChunkSize, int overlap, String... separators) {
        this(maxChunkSize, overlap, false, separators);
    }

    public CustomSeparatorDocumentSplitter(int maxChunkSize, int overlap, boolean useRegex, String... separators) {
        super(maxChunkSize, overlap);
        if (separators == null || separators.length == 0) {
            throw new IllegalArgumentException("At least one separator must be provided");
        }
        this.separators = separators.clone();
        this.useRegex = useRegex;
    }

    @Override
    public List<Document> split(Document document) {
        String content = document.getText();
        if (content == null || content.trim().isEmpty()) {
            return List.of(document);
        }

        List<String> chunks = splitText(content);
        List<String> mergedChunks = mergeChunks(chunks);

        List<Document> result = new ArrayList<>();
        for (int i = 0; i < mergedChunks.size(); i++) {
            result.add(createChunk(mergedChunks.get(i), document.getMetadata(), i));
        }

        return result;
    }

    private List<String> splitText(String text) {
        List<String> result = new ArrayList<>();
        splitRecursively(text, 0, result);
        return result;
    }

    private void splitRecursively(String text, int separatorIndex, List<String> result) {
        if (separatorIndex >= separators.length) {
            // 没有更多分隔符，直接添加
            if (!text.trim().isEmpty()) {
                result.add(text);
            }
            return;
        }

        String separator = separators[separatorIndex];
        String[] parts;

        if (useRegex) {
            parts = text.split(separator);
        } else {
            parts = text.split(Pattern.quote(separator));
        }

        if (parts.length <= 1) {
            // 当前分隔符无法分割，尝试下一个
            splitRecursively(text, separatorIndex + 1, result);
            return;
        }

        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }

            if (part.length() <= chunkSize) {
                result.add(part);
            } else {
                // 部分仍然太大，使用下一个分隔符继续分割
                splitRecursively(part, separatorIndex + 1, result);
            }
        }
    }
}
