package com.litevar.agent.openai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author uncle
 * @since 2025/2/20 17:28
 */
@Data
public class JsonObjectSchema implements JsonSchemaElement {
    private final String type = "object";
    private String description;
    private List<String> required;
    private Map<String, JsonSchemaElement> properties;
    @JsonProperty("additionalProperties")
    private final boolean additionalProperties = false;
}
