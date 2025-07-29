package com.litevar.agent.rest.springai.document;


import com.litevar.agent.rest.springai.document.splitter.CustomSeparatorDocumentSplitter;
import com.litevar.agent.rest.springai.document.splitter.RecursiveDocumentSplitter;

/**
 * @author reid
 * @since 2025/6/16
 */

public class DocumentSplitterFactory {

    /**
     * 创建文档分隔器
     * @param chunkSize 最大块大小
     * @param overlapSize 重叠大小
     * @param separators 自定义分隔符（可选）
     * @return 文档分隔器实例
     */
    public static DocumentSplitter create(int chunkSize, int overlapSize, String... separators) {
        if (separators != null && separators.length > 0) {
            return new CustomSeparatorDocumentSplitter(chunkSize, overlapSize, separators);
        } else {
            return new RecursiveDocumentSplitter(chunkSize, overlapSize);
        }
    }

    /**
     * 创建递归分隔器
     */
    public static DocumentSplitter createRecursive(int chunkSize, int overlapSize) {
        return new RecursiveDocumentSplitter(chunkSize, overlapSize);
    }

    /**
     * 创建自定义分隔符分隔器
     */
    public static DocumentSplitter createCustom(int chunkSize, int overlapSize, String... separators) {
        return new CustomSeparatorDocumentSplitter(chunkSize, overlapSize, separators);
    }

    /**
     * 创建正则表达式分隔器
     */
    public static DocumentSplitter createRegex(int chunkSize, int overlapSize, String... separators) {
        return new CustomSeparatorDocumentSplitter(chunkSize, overlapSize, true, separators);
    }
}
