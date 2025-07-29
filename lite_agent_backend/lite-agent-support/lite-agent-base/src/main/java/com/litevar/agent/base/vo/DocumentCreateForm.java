package com.litevar.agent.base.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author reid
 * @since 2/26/25
 */

@Data
public class DocumentCreateForm {
    @NotBlank(message = "workspaceId is required")
    private String workspaceId;
    /**
     * Document name
     */
    private String name;

    /**
     * Document source type: INPUT, FILE, HTML
     */
    @NotBlank(message = "dataSourceType is required")
    private String dataSourceType;
    /**
     * input content
     */
    private String content = "";
    /**
     * html url
     */
    private List<String> htmlUrl = Collections.emptyList();
    /**
     * upload file id
     */
    private String fileId = "";

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

}
