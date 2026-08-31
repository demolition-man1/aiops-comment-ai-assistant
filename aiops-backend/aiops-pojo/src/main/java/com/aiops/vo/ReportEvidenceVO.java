package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportEvidenceVO {
    private String sourceType;
    private Long sourceId;
    private String title;
    private Double score;
    private String retrievalVersion;
}
