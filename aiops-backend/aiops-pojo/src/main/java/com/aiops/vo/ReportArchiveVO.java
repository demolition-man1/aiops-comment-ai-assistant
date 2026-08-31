package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "报告归档响应")
public class ReportArchiveVO {
    private Long archiveId;
    private Long sourceReportId;
    private Long taskId;
    private String targetType;
    private String targetId;
    private String reportTitle;
    private String consumerPainPoints;
    private String productAdvantages;
    private String productDisadvantages;
    private String operationSuggestions;
    private String copywritingSuggestions;
    private String serviceSuggestions;
    private String riskTips;
    private String fullReport;
    private String modelName;
    private LocalDateTime reportCreateTime;
    private String archiveStatus;
    private String archiveRemark;
    private Long archivedBy;
    private LocalDateTime archiveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ReportEvidenceVO> evidence = List.of();

    public ReportArchiveVO(Long archiveId, Long sourceReportId, Long taskId, String targetType, String targetId,
                           String reportTitle, String consumerPainPoints, String productAdvantages,
                           String productDisadvantages, String operationSuggestions, String copywritingSuggestions,
                           String serviceSuggestions, String riskTips, String fullReport, String modelName,
                           LocalDateTime reportCreateTime, String archiveStatus, String archiveRemark, Long archivedBy,
                           LocalDateTime archiveTime, LocalDateTime createTime, LocalDateTime updateTime) {
        this(archiveId, sourceReportId, taskId, targetType, targetId, reportTitle, consumerPainPoints,
                productAdvantages, productDisadvantages, operationSuggestions, copywritingSuggestions,
                serviceSuggestions, riskTips, fullReport, modelName, reportCreateTime, archiveStatus, archiveRemark,
                archivedBy, archiveTime, createTime, updateTime, List.of());
    }

    public ReportArchiveVO(Long archiveId, Long sourceReportId, Long taskId, String targetType, String targetId,
                           String reportTitle, String consumerPainPoints, String productAdvantages,
                           String productDisadvantages, String operationSuggestions, String copywritingSuggestions,
                           String serviceSuggestions, String riskTips, String fullReport, String modelName,
                           LocalDateTime reportCreateTime, String archiveStatus, String archiveRemark, Long archivedBy,
                           LocalDateTime archiveTime, LocalDateTime createTime, LocalDateTime updateTime,
                           List<ReportEvidenceVO> evidence) {
        this.archiveId = archiveId;
        this.sourceReportId = sourceReportId;
        this.taskId = taskId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reportTitle = reportTitle;
        this.consumerPainPoints = consumerPainPoints;
        this.productAdvantages = productAdvantages;
        this.productDisadvantages = productDisadvantages;
        this.operationSuggestions = operationSuggestions;
        this.copywritingSuggestions = copywritingSuggestions;
        this.serviceSuggestions = serviceSuggestions;
        this.riskTips = riskTips;
        this.fullReport = fullReport;
        this.modelName = modelName;
        this.reportCreateTime = reportCreateTime;
        this.archiveStatus = archiveStatus;
        this.archiveRemark = archiveRemark;
        this.archivedBy = archivedBy;
        this.archiveTime = archiveTime;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
