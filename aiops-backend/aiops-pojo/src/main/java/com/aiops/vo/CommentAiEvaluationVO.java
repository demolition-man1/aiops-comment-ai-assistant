package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论 AI Shadow 评估指标")
public class CommentAiEvaluationVO {
    private Boolean qualityReady;
    private Integer sampleCount;
    private Integer annotatedCount;
    private Integer attemptedCallCount;
    private Integer successfulCallCount;
    private Integer failedCallCount;
    private Double annotationCoverage;
    private Double jsonValidRate;
    private Double evidenceValidRate;
    private Double callSuccessRate;
    private Integer totalTokens;
    private Integer estimatedTokenRowCount;
    private Double averageLatencyMs;
    private Boolean budgetStopped;
    private MetricBlock rule;
    private MetricBlock ai;
    private MetricBlock delta;

    @Data
    @Schema(description = "按来源计算的质量指标")
    public static class MetricBlock {
        private Double sentimentAccuracy;
        private Double problemMicroF1;
        private Double problemMacroF1;
    }
}
