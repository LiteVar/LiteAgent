package com.litevar.agent.base.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Form for creating a new dataset.
 */
@Data
public class DatasetCreateForm {
    /**
     * Dataset name
     */
    @NotBlank(message = "Dataset name cannot be empty")
    private String name;

    /**
     * Icon URL
     */
    private String icon = "";

    /**
     * Description
     */
    private String description = "";
    /**
     * LlmModel id
     */
    @NotBlank(message = "LlmModel id is required")
    private String llmModelId;
    /**
     * Embedding model name: text-embedding-3-small,text-embedding-3-large,text-embedding-ada-002
     */
    @NotBlank(message = "Embedding model name is required")
    private String embeddingModel;

    /**
     * Embedding model provider
     */
    private String embeddingModelProvider;

    /**
     * Number of top retrieval results
     */
    @NotNull(message = "Retrieval top K is required")
    @Min(value = 1, message = "Top K must be at least 1")
    @Max(value = 20, message = "Top K cannot exceed 20")
    private Integer retrievalTopK = 10;

    /**
     * Score threshold for retrieval results
     */
    @NotNull(message = "Retrieval score threshold is required")
    @Min(value = 0, message = "Score threshold must be at least 0")
    @Max(value = 1, message = "Score threshold cannot exceed 1")
    private Double retrievalScoreThreshold = 0.5;
}