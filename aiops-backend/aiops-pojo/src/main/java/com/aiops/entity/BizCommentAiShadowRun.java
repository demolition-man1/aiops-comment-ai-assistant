package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_comment_ai_shadow_run")
public class BizCommentAiShadowRun {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
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
    private LocalDateTime updateTime;
}
