package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.comment-ai-hybrid")
public class CommentAiHybridProperties {
    private String mode = "rule";
    private Double minConfidence = 0.80D;
    private Integer minAnnotated = 50;
    private Double minAnnotationCoverage = 0.80D;
    private Double minCallSuccessRate = 0.95D;
    private Double minJsonValidRate = 0.98D;
    private Double minEvidenceValidRate = 0.98D;
    private Double maxSentimentAccuracyDrop = 0.02D;
    private Double minProblemMicroF1Gain = 0.05D;
}
