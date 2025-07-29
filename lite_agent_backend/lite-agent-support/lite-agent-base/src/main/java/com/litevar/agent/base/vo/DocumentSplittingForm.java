package com.litevar.agent.base.vo;

import lombok.Data;

/**
 * @author reid
 * @since 2/27/25
 */

@Data
public class DocumentSplittingForm {
    /**
     * default chunk size
     */
    private Integer chunkSize = 500;
    /**
     * default Separator to split the document.
     */
    private String separator = "\n\n";

    /**
     * Metadata to be attached to the document, JSON string.
     */
    private String metadata = "";

    /**
     * Flag to indicate it is preview or confirm splitting process.
     */
    private Boolean previewFlag = Boolean.TRUE;
}
