package com.litevar.agent.rest.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author reid
 * @since 3/14/25
 */
public class TikToken {
    static final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
    static final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    public static int countTokens(String text) {
        return encoding.countTokens(text);
    }

    public static List<String> splitByTokens(String text, int maxTokensPerChunk) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        if (maxTokensPerChunk <= 0) {
            throw new IllegalArgumentException("maxTokensPerChunk must be greater than zero");
        }

        IntArrayList tokens = encoding.encode(text);
        int totalTokens = tokens.size();
        if (totalTokens <= maxTokensPerChunk) {
            return Collections.singletonList(text);
        }

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < totalTokens; start += maxTokensPerChunk) {
            int endExclusive = Math.min(totalTokens, start + maxTokensPerChunk);
            IntArrayList subTokens = new IntArrayList(endExclusive - start);
            for (int i = start; i < endExclusive; i++) {
                subTokens.add(tokens.get(i));
            }
            chunks.add(encoding.decode(subTokens));
        }
        return chunks;
    }
}
