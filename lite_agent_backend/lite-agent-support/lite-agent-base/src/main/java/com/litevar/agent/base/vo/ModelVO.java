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
     * 模型类型: text, embedding, asr, tts, image...
     */
    @NotBlank(message = "type is required")
    private String type;

}
