package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationReportVO {
    private Long reportId;
    private String reportTitle;
    private String consumerPainPoints;
    private String productAdvantages;
    private String productDisadvantages;
    private String operationSuggestions;
    private String copywritingSuggestions;
    private String serviceSuggestions;
    private String fullReport;
    private String modelName;
}

