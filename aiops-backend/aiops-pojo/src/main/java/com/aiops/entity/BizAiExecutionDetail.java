package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_ai_execution_detail")
public class BizAiExecutionDetail {
    @TableId
    private Long taskId;
    private String businessKey;
    private String idempotencyHash;
    private String requestHash;
    private String jobStage;
    private Integer attemptCount;
    private Long parentTaskId;
    private Integer cancelRequested;
    private String resultType;
    private Long resultId;
    private String modelName;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Integer tokenUsageEstimated;
    private BigDecimal estimatedCost;
    private Long queueLatencyMs;
    private Long providerLatencyMs;
    private Long totalLatencyMs;
    private String errorCode;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
