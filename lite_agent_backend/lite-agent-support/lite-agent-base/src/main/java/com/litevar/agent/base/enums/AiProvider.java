package com.litevar.agent.base.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author reid
 * @since 2025/6/26
 */

@Getter
@AllArgsConstructor
public enum AiProvider {
    OPENAI("openai", "OpenAI"),
//    OPENAI_COMPATIBLE("openai_compatible", "OpenAI兼容"),
    DASHSCOPE("dashscope", "通义千问"),
//    DEEPSEEK("deepseek", "深度求索"),
    OTHERS("others", "其他"),
    ;

    private final String value;
    private final String name;

    public static AiProvider of(String value) {
        for (AiProvider provider : values()) {
            if (provider.value.equals(value)) {
                return provider;
            }
        }
        return null;
    }

    public static Map<String, String> providers() {
        return Arrays.stream(AiProvider.values())
            .collect(Collectors.toMap(AiProvider::getValue, AiProvider::getName));
    }

}
