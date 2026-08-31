package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_operation_report_evidence")
public class BizOperationReportEvidence {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private Double relevanceScore;
    private String retrievalVersion;
    private LocalDateTime createTime;
}
