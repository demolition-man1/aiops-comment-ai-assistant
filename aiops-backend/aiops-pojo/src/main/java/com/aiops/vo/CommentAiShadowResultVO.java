package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论 AI Shadow 样本结果，含可复用人工标注")
public class CommentAiShadowResultVO {
    private Long resultId;
    private Long runId;
    private Long commentId;
    private Integer sampleOrder;
    private Integer reviewScore;
    private String reviewContent;
    private String ruleSentiment;
    private String ruleProblemType;
    private String aiSentiment;
    private BigDecimal aiSentimentConfidence;
    private String aiPrimaryProblem;
    private List<String> aiProblems;
    private String aiEvidence;
    private Integer jsonValid;
    private Integer evidenceValid;
    private String callStatus;
    private String modelName;
    private Integer tokenUsage;
    private Integer tokenUsageEstimated;
    private Long latencyMs;
    private String errorMessage;
    private String manualSentiment;
    private List<String> manualProblemTypes;
    private String annotationNote;
    private LocalDateTime annotationTime;
}
