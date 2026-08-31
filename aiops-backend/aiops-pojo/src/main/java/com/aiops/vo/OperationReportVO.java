package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class OperationReportVO {
    private Long reportId;
    private String targetType;
    private String targetId;
    private String reportTitle;
    private String consumerPainPoints;
    private String productAdvantages;
    private String productDisadvantages;
    private String operationSuggestions;
    private String copywritingSuggestions;
    private String serviceSuggestions;
    private String fullReport;
    private String modelName;
    private LocalDateTime createTime;
    private List<ReportEvidenceVO> evidence = List.of();

    public OperationReportVO(Long reportId, String targetType, String targetId, String reportTitle,
                             String consumerPainPoints, String productAdvantages, String productDisadvantages,
                             String operationSuggestions, String copywritingSuggestions, String serviceSuggestions,
                             String fullReport, String modelName, LocalDateTime createTime) {
        this(reportId, targetType, targetId, reportTitle, consumerPainPoints, productAdvantages,
                productDisadvantages, operationSuggestions, copywritingSuggestions, serviceSuggestions,
                fullReport, modelName, createTime, List.of());
    }

    public OperationReportVO(Long reportId, String targetType, String targetId, String reportTitle,
                             String consumerPainPoints, String productAdvantages, String productDisadvantages,
                             String operationSuggestions, String copywritingSuggestions, String serviceSuggestions,
                             String fullReport, String modelName, LocalDateTime createTime,
                             List<ReportEvidenceVO> evidence) {
        this.reportId = reportId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reportTitle = reportTitle;
        this.consumerPainPoints = consumerPainPoints;
        this.productAdvantages = productAdvantages;
        this.productDisadvantages = productDisadvantages;
        this.operationSuggestions = operationSuggestions;
        this.copywritingSuggestions = copywritingSuggestions;
        this.serviceSuggestions = serviceSuggestions;
        this.fullReport = fullReport;
        this.modelName = modelName;
        this.createTime = createTime;
        this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}

