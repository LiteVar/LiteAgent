package com.litevar.agent.base.dto;

import lombok.Data;

/**
 * @author uncle
 * @since 2024/10/12 15:11
 */
@Data
public class ModelDTO {
    private String id;

    /**
     * 模型名称
     */
    private String name;
    /**
     * 别名
     */
    private String alias;
    /**
     * 模型访问url
     */
    private String baseUrl;
    /**
     * 模型访问key
     */
    private String apiKey;

    /**
     * 创建者id
     */
    private String userId;

    /**
     * 创建人名称
     */
    private String createUser;

    /**
     * 限制最大token
     */
    private Integer maxTokens;
    /**
     * 模型类型: LLM, embedding, asr, tts, image...
     */
    private String type = "llm";
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

    /**
     * 是否能编辑
     */
    private Boolean canEdit = Boolean.FALSE;

    /**
     * 是否能删除
     */
    private Boolean canDelete = Boolean.FALSE;

    /**
     * 是否能读
     */
    private Boolean canRead = Boolean.TRUE;
}
