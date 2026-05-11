package com.litevar.agent.openai.tool;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author uncle
 * @since 2025/2/20 17:27
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = JsonObjectSchema.class, name = "object"),
        @JsonSubTypes.Type(value = JsonArraySchema.class, name = "array"),
        @JsonSubTypes.Type(value = JsonStringSchema.class, name = "string"),
        @JsonSubTypes.Type(value = JsonIntegerSchema.class, name = "integer"),
        @JsonSubTypes.Type(value = JsonNumberSchema.class, name = "number"),
        @JsonSubTypes.Type(value = JsonBooleanSchema.class, name = "boolean")
})
public interface JsonSchemaElement {
}