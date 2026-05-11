package com.litevar.agent.openai.completion;

import com.litevar.agent.openai.completion.message.AgentOSMessage;
import com.litevar.agent.openai.tool.ToolSpecification;
import lombok.Data;

import java.util.List;

/**
 * @author uncle
 * @since 2026/2/11 16:52
 */
@Data
public class AgentOSCompletionParam {
    /**
     * 消息列表
     */
    private List<AgentOSMessage> messages;

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
    private Boolean stream = Boolean.FALSE;

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
