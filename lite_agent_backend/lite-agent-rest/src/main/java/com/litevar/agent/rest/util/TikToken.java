package com.litevar.agent.rest.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

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
}
