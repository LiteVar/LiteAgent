package com.litevar.agent.openai.completion;

import com.litevar.agent.openai.tool.ToolSpecification;
import lombok.Data;

import java.util.List;

/**
 * @author uncle
 * @since 2025/3/4 10:35
 */
@Data
public class ChatModelRequest {
    private String baseUrl;
    private String apiKey;
    private String contextId;

    /**
     * 模型
     */
    private String model;

    /**
     * 最大返回token数
     */
    private Integer maxCompletionTokens;

    /**
     * 输出格式
     */
    private JsonSchemaResponseFormat responseFormat;

    /**
     * default: 1
     * (0, 2)
     */
    private Double temperature;

    /**
     * default: 1
     * (0, 1)
     */
    private Double topP;

    /**
     * 可以调用的工具,最多支持128个
     */
    private List<ToolSpecification> tools;
}
