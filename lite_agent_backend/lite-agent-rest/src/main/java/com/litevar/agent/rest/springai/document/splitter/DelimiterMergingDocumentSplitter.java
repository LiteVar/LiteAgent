package com.litevar.agent.rest.springai.document.splitter;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits documents by a specific delimiter and then merges adjacent blocks while
 * respecting the configured chunk and overlap sizes.
 */
public class DelimiterMergingDocumentSplitter extends AbstractDocumentSplitter {

    private final Pattern splitPattern;

    public DelimiterMergingDocumentSplitter(int chunkSize, int overlapSize, String delimiter) {
        super(chunkSize, overlapSize);
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("delimiter must not be blank");
        }
        this.splitPattern = Pattern.compile(Pattern.quote(delimiter));
    }

    @Override
    public List<Document> splitAll(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document document : documents) {
            result.addAll(split(document));
        }
        return result;
    }

    @Override
    public List<Document> split(Document document) {
        String content = document.getText();
        if (content == null || content.isBlank()) {
            return List.of(document);
        }

        List<String> initialChunks = initialSplit(content);
        List<String> mergedChunks = mergeAdjacent(initialChunks);

        List<Document> result = new ArrayList<>();
        for (int i = 0; i < mergedChunks.size(); i++) {
            result.add(createChunk(mergedChunks.get(i), document.getMetadata(), i));
        }
        return result;
    }

    private List<String> initialSplit(String content) {
        String[] parts = splitPattern.split(content, -1);
        List<String> chunks = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }
        return chunks;
    }

    private List<String> mergeAdjacent(List<String> chunks) {
        List<String> merged = new ArrayList<>();
        if (chunks.isEmpty()) {
            return merged;
        }

        StringBuilder current = new StringBuilder();
        int maxCombined = chunkSize + overlapSize;

        for (String chunk : chunks) {
            List<String> parts = splitChunkIfNeeded(chunk, maxCombined);
            for (String part : parts) {
                if (current.length() == 0) {
                    current.append(part);
                    continue;
                }

                int candidateLength = current.length() + 1 + part.length();
                if (candidateLength <= maxCombined) {
                    current.append('\n').append(part);
                } else {
                    merged.add(current.toString().trim());
                    current = new StringBuilder(part);
                }
            }
        }

        if (current.length() > 0) {
            merged.add(current.toString().trim());
        }
        return merged;
    }

    private List<String> splitChunkIfNeeded(String chunk, int maxLen) {
        List<String> parts = new ArrayList<>();
        String remaining = chunk.trim();
        while (!remaining.isEmpty()) {
            if (remaining.length() <= maxLen) {
                parts.add(remaining);
                break;
            }
            int end = Math.min(maxLen, remaining.length());
            int split = findSplitPoint(remaining, end);
            parts.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }
        return parts;
    }

    private int findSplitPoint(String text, int maxLen) {
        int idx = maxLen;
        while (idx > 0 && !Character.isWhitespace(text.charAt(idx - 1))) {
            idx--;
        }
        if (idx <= maxLen / 2) {
            idx = maxLen;
        }
        return idx;
    }
}
