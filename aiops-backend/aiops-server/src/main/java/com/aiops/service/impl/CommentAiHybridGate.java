package com.aiops.service.impl;

import com.aiops.properties.CommentAiHybridProperties;
import com.aiops.vo.CommentAiEvaluationVO;
import com.aiops.vo.CommentAiHybridReadinessVO;

import java.util.ArrayList;
import java.util.List;

public class CommentAiHybridGate {

    private final CommentAiHybridProperties properties;

    public CommentAiHybridGate(CommentAiHybridProperties properties) {
        this.properties = properties;
    }

    public CommentAiHybridReadinessVO evaluate(CommentAiEvaluationVO evaluation) {
        List<String> failures = new ArrayList<>();
        if (value(evaluation == null ? null : evaluation.getAnnotatedCount()) < properties.getMinAnnotated()) {
            failures.add("minimum_annotated");
        }
        if (value(evaluation == null ? null : evaluation.getAnnotationCoverage()) < properties.getMinAnnotationCoverage()) {
            failures.add("annotation_coverage");
        }
        if (value(evaluation == null ? null : evaluation.getCallSuccessRate()) < properties.getMinCallSuccessRate()) {
            failures.add("call_success_rate");
        }
        if (value(evaluation == null ? null : evaluation.getJsonValidRate()) < properties.getMinJsonValidRate()) {
            failures.add("json_valid_rate");
        }
        if (value(evaluation == null ? null : evaluation.getEvidenceValidRate()) < properties.getMinEvidenceValidRate()) {
            failures.add("evidence_valid_rate");
        }
        double ruleSentiment = value(evaluation == null || evaluation.getRule() == null
                ? null : evaluation.getRule().getSentimentAccuracy());
        double aiSentiment = value(evaluation == null || evaluation.getAi() == null
                ? null : evaluation.getAi().getSentimentAccuracy());
        if (aiSentiment < ruleSentiment - properties.getMaxSentimentAccuracyDrop()) {
            failures.add("sentiment_accuracy");
        }
        double ruleProblemF1 = value(evaluation == null || evaluation.getRule() == null
                ? null : evaluation.getRule().getProblemMicroF1());
        double aiProblemF1 = value(evaluation == null || evaluation.getAi() == null
                ? null : evaluation.getAi().getProblemMicroF1());
        if (aiProblemF1 < ruleProblemF1 + properties.getMinProblemMicroF1Gain()) {
            failures.add("problem_micro_f1");
        }
        if (Boolean.TRUE.equals(evaluation == null ? null : evaluation.getBudgetStopped())) {
            failures.add("budget_stopped");
        }
        return new CommentAiHybridReadinessVO(failures.isEmpty(), List.copyOf(failures), 0, 0, properties.getMode());
    }

    private double value(Number value) {
        return value == null ? 0D : value.doubleValue();
    }
}
