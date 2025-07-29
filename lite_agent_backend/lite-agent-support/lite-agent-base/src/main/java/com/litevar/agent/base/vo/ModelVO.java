package com.litevar.agent.base.vo;

import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * @author uncle
 * @since 2024/8/9 17:27
 */
@Data
public class ModelVO {

    @NotBlank(groups = UpdateAction.class)
    @Null(groups = AddAction.class)
    private String id;
    /**
     * 别名
     */
    @NotBlank
    private String alias;

    /**
     * 模型名字
     */
    @NotBlank
    private String name;
    /**
     * 请求url
     */
    @NotBlank
    private String baseUrl;
    /**
     * 接口key
     */
    @NotBlank
    private String apiKey;

    /**
     * 限制最大token
     */
    @Min(value = 1L)
    private Integer maxTokens;

    /**
     * 模型类型: LLM, embedding, asr, tts, image...
     */
    @NotBlank(message = "type is required")
    private String type;

    /**
     * 模型供应商: openai, dashscope, deepseek, prm, others ...
     */
    private String provider = "openai";
    /**
     * 字段映射,JSON字符串格式(不兼容OpenAI的模型可以设置字段映射,后端请求时动态修改请求体)
     */
    private String fieldMapping = "";
    /**
     * 响应格式: wav, mp3, pcm...
     * 默认wav
     */
    private String responseFormat = "wav";

    /**
     * 是否支持auto agent使用
     */
    private Boolean autoAgent = Boolean.FALSE;

    /**
     * 是否支持工具调用
     */
    private Boolean toolInvoke = Boolean.TRUE;
    /**
     * 是否支持深度思考
     */
    private Boolean deepThink = Boolean.FALSE;
}
