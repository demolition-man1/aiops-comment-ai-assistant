package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI 异步任务详情")
public class AiJobVO {
    private Long jobId;
    private String jobType;
    private String targetType;
    private String targetId;
    private String taskStatus;
    private String jobStage;
    private Integer progress;
    private String resultType;
    private Long resultId;
    private Integer attemptCount;
    private Boolean cancelRequested;
    private Long queueLatencyMs;
    private Long providerLatencyMs;
    private Long totalLatencyMs;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
