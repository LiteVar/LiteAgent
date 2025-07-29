package com.litevar.agent.openai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author uncle
 * @since 2025/2/20 17:43
 */
@Data
public class JsonNumberSchema implements JsonSchemaElement {
    private final String type = "number";
    private String description;
    @JsonProperty("enum")
    private List<Object> enums;
    @JsonProperty("default")
    private Object defaultValue;
}
