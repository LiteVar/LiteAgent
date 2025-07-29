package com.litevar.agent.rest.springai.document.splitter;

import com.litevar.agent.rest.springai.document.DocumentSplitter;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author reid
 * @since 2025/6/16
 */

abstract class AbstractDocumentSplitter implements DocumentSplitter {

    protected final int chunkSize;
    protected final int overlapSize;

    public AbstractDocumentSplitter(int chunkSize, int overlapSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (overlapSize < 0 || overlapSize >= chunkSize) {
            throw new IllegalArgumentException("overlapSize must be non-negative and less than chunkSize");
        }
        if (overlapSize == 0) {
            overlapSize = chunkSize / 10;
        }
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize;
    }

    @Override
    public List<Document> splitAll(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document document : documents) {
            result.addAll(split(document));
        }
        return result;
    }

    /**
     * 创建文档块
     */
    protected Document createChunk(String content, Map<String, Object> originalMetadata, int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>(originalMetadata);
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_size", content.length());
        return new Document(content, metadata);
    }

    /**
     * 合并文本块，考虑重叠
     */
    protected List<String> mergeChunks(List<String> chunks) {
        if (chunks.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String chunk : chunks) {
            // 如果添加当前块会超过最大长度，先保存当前块
            if (currentChunk.length() + chunk.length() > chunkSize && !currentChunk.isEmpty()) {
                result.add(currentChunk.toString().trim());

                // 计算重叠部分
                String currentContent = currentChunk.toString();
                int overlapStart = Math.max(0, currentContent.length() - overlapSize);
                currentChunk = new StringBuilder(currentContent.substring(overlapStart));
            }

            // 如果单个块就超过最大长度，需要进一步拆分
            if (chunk.length() > chunkSize) {
                // 先保存当前累积的内容
                if (!currentChunk.isEmpty()) {
                    result.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                // 拆分大块
                List<String> subChunks = splitLargeChunk(chunk);
                for (int j = 0; j < subChunks.size(); j++) {
                    if (j == subChunks.size() - 1) {
                        // 最后一个子块添加到当前块中
                        currentChunk.append(subChunks.get(j));
                    } else {
                        result.add(subChunks.get(j));
                    }
                }
            } else {
                if (!currentChunk.isEmpty() && !currentChunk.toString().endsWith(" ") && !chunk.startsWith(" ")) {
                    currentChunk.append(" ");
                }
                currentChunk.append(chunk);
            }
        }

        // 添加最后的块
        if (!currentChunk.isEmpty()) {
            result.add(currentChunk.toString().trim());
        }

        return result;
    }

    /**
     * 拆分超大文本块
     */
    private List<String> splitLargeChunk(String chunk) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < chunk.length()) {
            int end = Math.min(start + chunkSize, chunk.length());

            // 尝试在单词边界处分割
            if (end < chunk.length()) {
                int lastSpace = chunk.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String subChunk = chunk.substring(start, end);
            result.add(subChunk);

            // 计算下一个开始位置，考虑重叠
            start = Math.max(start + 1, end - overlapSize);
        }

        return result;
    }
}
