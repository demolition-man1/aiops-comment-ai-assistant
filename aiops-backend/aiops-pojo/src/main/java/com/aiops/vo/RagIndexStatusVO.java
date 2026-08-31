package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagIndexStatusVO {
    private Boolean enabled;
    private Boolean ready;
    private String state;
    private String collection;
    private Integer documentCount;
    private Integer problemSolutionCount;
    private Integer historicalReplyCount;
    private Integer reviewEvidenceCount;
    private String embeddingModel;
    private String lastReindexAt;
    private String lastError;
}
