package com.litevar.agent.openai.completion;

import com.litevar.agent.openai.tool.JsonObjectSchema;
import lombok.Data;

/**
 * response_format: type=json_schema
 * to use Structured Outputs,all fields or function parameters
 * must be specified as required
 *
 * @author uncle
 * @since 2025/2/21 16:41
 */
@Data
public class JsonSchemaResponseFormat {
    private final String type = "json_schema";
    private JsonSchema jsonSchema;

    @Data
    public static class JsonSchema {
        private String name;
        private final boolean strict = true;
        private JsonObjectSchema schema;
        private String description;
    }
}
