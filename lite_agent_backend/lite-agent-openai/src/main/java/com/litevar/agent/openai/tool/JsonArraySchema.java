package com.litevar.agent.openai.tool;

import lombok.Data;

/**
 * @author uncle
 * @since 2025/2/20 17:30
 */
@Data
public class JsonArraySchema implements JsonSchemaElement {
    private final String type = "array";
    private String description;
    private JsonSchemaElement items;
}
