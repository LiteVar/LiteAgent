package com.litevar.agent.rest.langchain4j;

import dev.langchain4j.service.TokenStream;

/**
 * @author uncle
 * @since 2024/10/15 15:32
 */
public interface Assistant {
    TokenStream chat(String message);
}