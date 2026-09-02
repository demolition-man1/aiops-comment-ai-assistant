package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_ai_call_log")
public class BizAiCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long userId;
    private String businessType;
    private String targetType;
    private String targetId;
    private Long promptTemplateId;
    private String modelName;
    private String callStatus;
    private Integer tokenUsage;
    private BigDecimal estimatedCost;
    private Long latencyMs;
    private Long queueLatencyMs;
    private Long totalLatencyMs;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
}
