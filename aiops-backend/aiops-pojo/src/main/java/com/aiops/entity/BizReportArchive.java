package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_report_archive")
public class BizReportArchive {
    @TableId(type = IdType.AUTO)
    private Long id;
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
