package com.litevar.agent.rest.springai.document;

import lombok.Getter;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Spring AI Document文本分隔器
 * 支持多级分割、智能合并和重叠处理
 */
@Getter
public class SimpleDocumentSplitter {

    // Getters
    private final String separator;
    private final int chunkSize;
    private final int overlapSize;

    /**
     * 构造函数
     * @param separator 分隔符
     * @param chunkSize 文本块大小
     * @param overlapSize 重叠大小（可选，默认为chunkSize/10）
     */
    public SimpleDocumentSplitter(String separator, int chunkSize, Integer overlapSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize必须大于0");
        }

        this.separator = separator != null ? separator : "\n";
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize != null ? overlapSize : Math.max(0, chunkSize / 10);

        if (this.overlapSize >= chunkSize) {
            throw new IllegalArgumentException("overlapSize不能大于或等于chunkSize");
        }
    }

    /**
     * 构造函数（使用默认重叠大小）
     */
    public SimpleDocumentSplitter(String separator, int chunkSize) {
        this(separator, chunkSize, null);
    }

    /**
     * 分割文档列表的主方法
     * @param documents 要分割的文档列表
     * @return 分割后的文档列表
     */
    public List<Document> split(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Document> result = new ArrayList<>();

        for (Document document : documents) {
            List<Document> splitDocuments = splitDocument(document);
            result.addAll(splitDocuments);
        }

        return result;
    }

    /**
     * 分割单个文档
     * @param document 要分割的文档
     * @return 分割后的文档列表
     */
    public List<Document> splitDocument(Document document) {
        if (document == null || document.getText() == null || document.getText().isEmpty()) {
            return Collections.singletonList(document);
        }

        String text = document.getText();
        Map<String, Object> originalMetadata = document.getMetadata();

        // 分割文本内容
        List<String> textChunks = splitText(text);

        // 转换为Document对象列表
        List<Document> documentChunks = new ArrayList<>();

        for (String chunk : textChunks) {
            // 创建新的元数据
//            DocumentMetadata chunkMetadata = createChunkMetadata(originalMetadata, i, textChunks.size(), chunk);

            // 创建新的Document
            Document chunkDocument = new Document(chunk, originalMetadata);
            documentChunks.add(chunkDocument);
        }

        return documentChunks;
    }

    /**
     * 分割文本的核心逻辑（从原有的字符串分隔器中提取）
     */
    private List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 第一步：根据分隔符进行初始分割
        List<String> initialChunks = initialSplit(text);

        // 第二步：处理超长片段和合并短片段
//        List<String> processedChunks = processChunks(initialChunks);
        return processChunks(initialChunks);

//        // 第三步：添加重叠内容
//        return addOverlap(processedChunks);
    }

    // 以下方法与之前的TextSplitter实现相同
    private List<String> initialSplit(String text) {
        List<String> chunks = new ArrayList<>();

        if (separator.isEmpty()) {
            chunks.add(text);
            return chunks;
        }

        String[] parts = text.split(Pattern.quote(separator), -1);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                chunks.add(part);
            } else if (i < parts.length - 1) {
                chunks.add("");
            }
        }

        return chunks;
    }

    private List<String> processChunks(List<String> chunks) {
        List<String> result = new ArrayList<>();

        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                continue;
            }

            if (chunk.length() > chunkSize) {
                result.addAll(splitLongChunk(chunk));
            } else {
                result.add(chunk);
            }
        }

        return mergeShortChunks(result);
    }

    private List<String> splitLongChunk(String chunk) {
        List<String> subChunks = new ArrayList<>();
        List<String> sentences = splitBySentence(chunk);
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    subChunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                subChunks.addAll(forceSplit(sentence));
            } else if (currentChunk.length() + sentence.length() + 1 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    subChunks.add(currentChunk.toString().trim());
                }
                currentChunk = new StringBuilder(sentence);
            }
        }

        if (currentChunk.length() > 0) {
            subChunks.add(currentChunk.toString().trim());
        }

        return subChunks;
    }

    private List<String> splitBySentence(String text) {
        String[] sentences = text.split("(?<=[.!?。！？])\\s+");
        List<String> result = new ArrayList<>();

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (!sentence.isEmpty()) {
                result.add(sentence);
            }
        }

        return result.isEmpty() ? Arrays.asList(text) : result;
    }

    private List<String> forceSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                int wordBoundary = text.lastIndexOf(' ', end);
                if (wordBoundary > start) {
                    end = wordBoundary;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;

            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }

        return chunks;
    }

    private List<String> mergeShortChunks(List<String> chunks) {
        if (chunks.isEmpty()) {
            return chunks;
        }

        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                continue;
            }

            if (current.length() == 0) {
                current.append(chunk);
            } else if (current.length() + chunk.length() + 1 <= chunkSize) {
                current.append(" ").append(chunk);
            } else {
                merged.add(current.toString());
                current = new StringBuilder(chunk);
            }
        }

        if (current.length() > 0) {
            merged.add(current.toString());
        }

        return merged;
    }

    private List<String> addOverlap(List<String> chunks) {
        if (chunks.size() <= 1 || overlapSize == 0) {
            return chunks;
        }

        List<String> result = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            StringBuilder overlappedChunk = new StringBuilder();

            // 添加前面的重叠内容
            if (i > 0) {
                String prevChunk = chunks.get(i - 1);
                String prevOverlap = extractOverlap(prevChunk, false);
                if (!prevOverlap.isEmpty()) {
                    overlappedChunk.append(prevOverlap).append(" ");
                }
            }

            // 添加当前块
            overlappedChunk.append(chunk);

            // 添加后面的重叠内容
            if (i < chunks.size() - 1) {
                String nextChunk = chunks.get(i + 1);
                String nextOverlap = extractOverlap(nextChunk, true);
                if (!nextOverlap.isEmpty()) {
                    overlappedChunk.append(" ").append(nextOverlap);
                }
            }

            result.add(overlappedChunk.toString());
        }

        return result;
    }

    private String extractOverlap(String chunk, boolean fromStart) {
        if (chunk.length() <= overlapSize) {
            return chunk;
        }

        if (fromStart) {
            int end = overlapSize;
            while (end < chunk.length() && !Character.isWhitespace(chunk.charAt(end))) {
                end++;
            }
            return chunk.substring(0, Math.min(end, chunk.length())).trim();
        } else {
            int start = chunk.length() - overlapSize;
            while (start > 0 && !Character.isWhitespace(chunk.charAt(start))) {
                start--;
            }
            return chunk.substring(Math.max(0, start), chunk.length()).trim();
        }
    }

}
