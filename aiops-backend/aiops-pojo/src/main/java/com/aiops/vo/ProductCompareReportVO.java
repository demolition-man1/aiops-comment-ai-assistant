package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompareReportVO {
    private Long reportId;
    private String leftProductId;
    private String rightProductId;
    private String metricSnapshot;
    private String compareSummary;
    private String advantageAnalysis;
    private String riskAnalysis;
    private String operationSuggestions;
    private String modelName;
    private LocalDateTime createTime;
}
