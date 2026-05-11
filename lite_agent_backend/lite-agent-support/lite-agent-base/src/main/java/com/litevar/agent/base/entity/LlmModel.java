package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionLogic;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大语言模型表
 *
 * @author reid
 * @since 2024/8/1
 */
@Data
@CollectionName("llm_model")
public class LlmModel {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 模型名称
     */
    private String name;
    /**
     * 别名(方便辨别)
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
     * 工作空间id
     */
    @MongoIndex
    private String workspaceId;
    /**
     * 模型类型: LLM, embedding, asr, tts, image...
     */
    private String type = "LLM";
    /**
     * 模型供应商: openai, dashscope, deepseek, prm, others ...
     */
    private String provider = "openai";
    /**
     * 字段映射,JSON字符串格式(不兼容OpenAI的模型可以设置字段映射,后端请求时动态修改请求体)
     */
    private String fieldMapping = "";
    /**
     * 语音模型的响应格式,如:
     * 音转文: text/srt/vtt/json...
     * 文转音: pcm/wav/mp3/mpeg...
     */
    private String responseFormat = "";

    /**
     * 限制一次响应最大输出token
     */
    private Integer maxTokens;

    /**
     * 模型上下文窗口大小
     */
    private Integer contextWindows;

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
     * 是否支持视觉识别(VL模型)
     */
    private Boolean vision = Boolean.FALSE;
    /**
     * 是否支持流式输出
     */
    private Boolean streamable = Boolean.FALSE;

    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @CollectionField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @CollectionLogic
    private String deleted = "0";

    /**
     * 模型状态: 0-待启用, 1-启用, 2-禁用
     */
    private Integer status = 0;
}
