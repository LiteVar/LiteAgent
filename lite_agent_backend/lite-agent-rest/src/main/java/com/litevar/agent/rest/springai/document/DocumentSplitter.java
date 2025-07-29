package com.litevar.agent.rest.springai.document;


import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @author reid
 * @since 2025/6/11
 */

public interface DocumentSplitter {
    List<Document> split(Document document);
    List<Document> splitAll(List<Document> documents);
}
