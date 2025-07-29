package com.litevar.agent.openai.completion;

import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.tool.ToolSpecification;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Chat Completion接口请求参数
 *
 * @author uncle
 * @since 2025/2/13 10:51
 */
@Data
public class CompletionRequestParam {
    /**
     * 消息列表
     */
    private List<Message> messages;

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
     * 是否启用流式 default: false
     */
    private boolean stream;

    /**
     * 当stream=true时,可配置返回usage数据
     */
    private Map<String, Object> streamOptions;

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
