package com.litevar.agent.base.vo;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * @author reid
 * @since 2025/6/26
 */

@Data
public class SpeechForm {
    /**
     * 语音模型ID
     */
    @NotBlank
    private String modelId;
    /**
     * 语音参数
     */
    private Map<String, Object> params;
}
