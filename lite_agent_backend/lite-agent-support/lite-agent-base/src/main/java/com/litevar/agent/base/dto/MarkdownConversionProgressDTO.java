package com.litevar.agent.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Progress payload stored for markdown conversion jobs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownConversionProgressDTO {
    private String fileId;
    /** progress percentage (0-100). */
    private double progress;
    /** symbolic stage identifier, see Converter for values. */
    private String stage;
    /** human readable detail. */
    private String detail;
    /** job status e.g. RUNNING/COMPLETED/FAILED. */
    private String status;
}

