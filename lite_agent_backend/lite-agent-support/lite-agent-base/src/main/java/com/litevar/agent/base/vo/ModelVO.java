package com.litevar.agent.base.vo;

import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "alias字段不能为空")
    private String alias;

    /**
     * 模型名字
     */
    @NotBlank(message = "name字段不能为空")
    private String name;
    /**
     * 请求url
     */
    @NotBlank(message = "baseUrl字段不能为空")
    private String baseUrl;
    /**
     * 接口key
     */
    @NotBlank(message = "apiKey字段不能为空")
    private String apiKey;

    /**
     * 限制最大token
     */
    @Min(value = 1L, message = "maxTokens必须大于0")
    private Integer maxTokens;

    /**
     * 模型类型: LLM, embedding, asr, tts, image...
     */
    @NotBlank(message = "type is required")
    @Pattern(regexp = "^(LLM|embedding|asr|tts|image)$", message = "type字段值无效，仅支持：LLM, embedding, asr, tts, image")
    private String type;

    /**
     * 模型供应商: openai, dashscope, deepseek, prm, others ...
     */
    @Pattern(regexp = "^(openai|dashscope|deepseek|prm|others)$", message = "provider字段值无效，仅支持：openai, dashscope, deepseek, prm, others")
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


    //=========== 以下字段为导入时用到

    /**
     * 相同的数据id
     */
    private String similarId;

    /**
     * 数据操作类型
     *
     * @see com.litevar.agent.base.enums.OperateTypeEnum
     */
    private Integer operate = 0;
}
