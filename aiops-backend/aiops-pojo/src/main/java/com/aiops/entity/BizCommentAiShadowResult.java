package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_comment_ai_shadow_result")
public class BizCommentAiShadowResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long commentId;
    private Integer sampleOrder;
    private String ruleSentiment;
    private String ruleProblemType;
    private String aiSentiment;
    private BigDecimal aiSentimentConfidence;
    private String aiPrimaryProblem;
    private String aiProblems;
    private String aiEvidence;
    private Integer jsonValid;
    private Integer evidenceValid;
    private String callStatus;
    private String modelName;
    private Integer tokenUsage;
    private Integer tokenUsageEstimated;
    private Long latencyMs;
    private String errorMessage;
    private LocalDateTime createTime;
}
