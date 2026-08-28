package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
