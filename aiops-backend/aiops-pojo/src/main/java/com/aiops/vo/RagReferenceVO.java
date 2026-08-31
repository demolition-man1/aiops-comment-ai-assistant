package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagReferenceVO {
    private String sourceType;
    private Long sourceId;
    private String title;
    private Double score;
}
