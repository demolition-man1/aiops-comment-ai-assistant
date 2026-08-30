package com.aiops.service.impl;

import com.aiops.properties.CommentAiHybridProperties;
import com.aiops.vo.CommentAiEvaluationVO;
import com.aiops.vo.CommentAiHybridReadinessVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentAiHybridGateTest {

    @Test
    void gateListsEveryFailedThresholdAndAcceptsACompletePassingEvaluation() {
        CommentAiHybridProperties properties = new CommentAiHybridProperties();
        CommentAiHybridGate gate = new CommentAiHybridGate(properties);

        CommentAiEvaluationVO failing = evaluation(49, 0.79, 0.94, 0.97, 0.97, 0.90, 0.85, -0.03, false);
        CommentAiHybridReadinessVO failingReadiness = gate.evaluate(failing);

        assertThat(failingReadiness.getReady()).isFalse();
        assertThat(failingReadiness.getFailures()).containsExactly(
                "minimum_annotated",
                "annotation_coverage",
                "call_success_rate",
                "json_valid_rate",
                "evidence_valid_rate",
                "sentiment_accuracy",
                "problem_micro_f1"
        );

        CommentAiHybridReadinessVO passingReadiness = gate.evaluate(
                evaluation(50, 0.80, 0.95, 0.98, 0.98, 0.90, 0.96, 0.01, false)
        );

        assertThat(passingReadiness.getReady()).isTrue();
        assertThat(passingReadiness.getFailures()).isEmpty();
    }

    @Test
    void gateRejectsBudgetStoppedEvenWhenAllOtherThresholdsPass() {
        CommentAiHybridReadinessVO readiness = new CommentAiHybridGate(new CommentAiHybridProperties())
                .evaluate(evaluation(50, 0.80, 0.95, 0.98, 0.98, 0.90, 0.96, 0.01, true));

        assertThat(readiness.getReady()).isFalse();
        assertThat(readiness.getFailures()).containsExactly("budget_stopped");
    }

    private CommentAiEvaluationVO evaluation(int annotatedCount, double annotationCoverage, double callSuccessRate,
                                             double jsonValidRate, double evidenceValidRate, double ruleSentimentAccuracy,
                                             double aiProblemMicroF1, double sentimentDelta, boolean budgetStopped) {
        CommentAiEvaluationVO evaluation = new CommentAiEvaluationVO();
        evaluation.setAnnotatedCount(annotatedCount);
        evaluation.setAnnotationCoverage(annotationCoverage);
        evaluation.setCallSuccessRate(callSuccessRate);
        evaluation.setJsonValidRate(jsonValidRate);
        evaluation.setEvidenceValidRate(evidenceValidRate);
        evaluation.setBudgetStopped(budgetStopped);
        CommentAiEvaluationVO.MetricBlock rule = new CommentAiEvaluationVO.MetricBlock();
        rule.setSentimentAccuracy(ruleSentimentAccuracy);
        rule.setProblemMicroF1(0.90);
        evaluation.setRule(rule);
        CommentAiEvaluationVO.MetricBlock ai = new CommentAiEvaluationVO.MetricBlock();
        ai.setSentimentAccuracy(ruleSentimentAccuracy + sentimentDelta);
        ai.setProblemMicroF1(aiProblemMicroF1);
        evaluation.setAi(ai);
        return evaluation;
    }
}
