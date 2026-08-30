package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论 AI Shadow 运行记录")
public class CommentAiShadowRunVO {
    private Long runId;
    private Long taskId;
    private String targetType;
    private String targetId;
    private Integer sampleSeed;
    private Integer requestedSampleSize;
    private Integer actualSampleSize;
    private Integer maxTotalTokens;
    private Integer totalCalls;
    private Integer successCount;
    private Integer failureCount;
    private Integer totalTokens;
    private Long latencyMs;
    private String runStatus;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
