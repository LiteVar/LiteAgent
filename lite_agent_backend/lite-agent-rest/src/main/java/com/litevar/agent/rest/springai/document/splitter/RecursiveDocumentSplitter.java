package com.litevar.agent.rest.springai.document.splitter;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author reid
 * @since 2025/6/16
 */

public class RecursiveDocumentSplitter extends AbstractDocumentSplitter {

    private static final String[] DEFAULT_SEPARATORS = {
        "\n\n",    // 段落分隔
        "\n",      // 行分隔
        " ",       // 单词分隔
        ""         // 字符分隔
    };

    private final String[] separators;

    public RecursiveDocumentSplitter(int maxChunkSize, int overlap) {
        this(maxChunkSize, overlap, DEFAULT_SEPARATORS);
    }

    public RecursiveDocumentSplitter(int maxChunkSize, int overlap, String[] separators) {
        super(maxChunkSize, overlap);
        this.separators = separators != null ? separators.clone() : DEFAULT_SEPARATORS;
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
        return splitTextRecursively(text, 0);
    }

    private List<String> splitTextRecursively(String text, int separatorIndex) {
        List<String> result = new ArrayList<>();

        // 如果文本足够小，直接返回
        if (text.length() <= chunkSize) {
            if (!text.trim().isEmpty()) {
                result.add(text);
            }
            return result;
        }

        // 如果没有更多分隔符，强制按字符分割
        if (separatorIndex >= separators.length) {
            return forceCharacterSplit(text);
        }

        String separator = separators[separatorIndex];
        String[] parts;

        if (separator.isEmpty()) {
            // 字符级分割
            return forceCharacterSplit(text);
        } else {
            parts = text.split(Pattern.quote(separator), -1);
        }

        // 如果当前分隔符无法有效分割，尝试下一个
        if (parts.length <= 1) {
            return splitTextRecursively(text, separatorIndex + 1);
        }

        // 处理分割后的各部分
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // 重新添加分隔符（除了最后一部分）
            if (i < parts.length - 1 && !separator.equals(" ")) {
                part += separator;
            }

            if (part.trim().isEmpty()) {
                continue;
            }

            if (part.length() <= chunkSize) {
                result.add(part);
            } else {
                // 部分仍然太大，递归处理
                result.addAll(splitTextRecursively(part, separatorIndex + 1));
            }
        }

        return result;
    }

    private List<String> forceCharacterSplit(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            start = Math.max(start + 1, end - overlapSize);
        }

        return result;
    }
}
