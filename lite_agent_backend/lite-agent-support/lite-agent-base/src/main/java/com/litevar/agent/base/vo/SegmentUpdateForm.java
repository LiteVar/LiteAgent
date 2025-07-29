package com.litevar.agent.base.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author reid
 * @since 3/4/25
 */

@Data
public class SegmentUpdateForm {

    @NotBlank(message = "content is required")
    private String content;

    private String metadata = "";
}
