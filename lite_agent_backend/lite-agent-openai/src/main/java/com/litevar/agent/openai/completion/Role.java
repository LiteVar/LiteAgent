package com.litevar.agent.openai.completion;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author uncle
 * @since 2025/2/13 12:08
 */
public enum Role {
    @JsonProperty("system")
    DEVELOPER,

    @JsonProperty("user")
    USER,

    @JsonProperty("assistant")
    ASSISTANT,

    @JsonProperty("tool")
    TOOL,


    ;
}
