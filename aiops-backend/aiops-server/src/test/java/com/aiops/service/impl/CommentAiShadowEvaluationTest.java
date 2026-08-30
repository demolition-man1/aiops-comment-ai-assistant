package com.aiops.service.impl;

import com.aiops.client.PythonAnalysisClient;
import com.aiops.dto.CommentAiAnnotationDTO;
import com.aiops.entity.BizCommentAiAnnotation;
import com.aiops.entity.BizCommentAiShadowRun;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCommentAiAnnotationMapper;
import com.aiops.mapper.BizCommentAiShadowResultMapper;
import com.aiops.mapper.BizCommentAiShadowRunMapper;
import com.aiops.service.AiCallLogService;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.CommentAiEvaluationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAiShadowEvaluationTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;
    @Mock
    private BizCommentAiShadowRunMapper runMapper;
    @Mock
    private BizCommentAiShadowResultMapper resultMapper;
    @Mock
    private BizCommentAiAnnotationMapper annotationMapper;
    @Mock
    private PythonAnalysisClient pythonAnalysisClient;
    @Mock
    private PromptTemplateService promptTemplateService;
    @Mock
    private AiCallLogService aiCallLogService;

    private CommentAiShadowServiceImpl service;

    @BeforeEach
    void setUp() {
        TaskExecutor taskExecutor = Runnable::run;
        service = new CommentAiShadowServiceImpl(
                taskMapper,
                runMapper,
                resultMapper,
                annotationMapper,
                pythonAnalysisClient,
                promptTemplateService,
                aiCallLogService,
                new ObjectMapper(),
                taskExecutor
        );
    }

    @Test
    void evaluationReturnsCountsWithoutCallingPythonWhenNoAnnotationExists() {
        when(runMapper.selectById(7L)).thenReturn(run());
        when(resultMapper.selectEvaluationRows(7L)).thenReturn(List.of(row(null)));

        CommentAiEvaluationVO result = service.evaluateRun(7L);

        assertThat(result.getQualityReady()).isFalse();
        assertThat(result.getSampleCount()).isEqualTo(1);
        assertThat(result.getAnnotatedCount()).isZero();
        assertThat(result.getTotalTokens()).isEqualTo(80);
        verify(pythonAnalysisClient, never()).evaluateCommentShadow(any());
    }

    @Test
    void evaluationKeepsRunMetricsConsistentWhenARequestFailedBeforeAnnotation() {
        when(runMapper.selectById(7L)).thenReturn(run());
        when(resultMapper.selectEvaluationRows(7L)).thenReturn(List.of(failedRow()));

        CommentAiEvaluationVO result = service.evaluateRun(7L);

        assertThat(result.getAttemptedCallCount()).isEqualTo(1);
        assertThat(result.getFailedCallCount()).isEqualTo(1);
        assertThat(result.getAverageLatencyMs()).isEqualTo(15.0);
        verify(pythonAnalysisClient, never()).evaluateCommentShadow(any());
    }

    @Test
    void evaluationPassesAnnotatedRowsToPythonAndMapsRuleAiMetricBlocks() {
        when(runMapper.selectById(7L)).thenReturn(run());
        when(resultMapper.selectEvaluationRows(7L)).thenReturn(List.of(row("negative")));
        when(pythonAnalysisClient.evaluateCommentShadow(any())).thenReturn(Map.of(
                "success", true,
                "evaluation", Map.of(
                        "qualityReady", true,
                        "counts", Map.of("sampleCount", 1, "annotatedCount", 1, "attemptedCallCount", 1,
                                "successfulCallCount", 1, "failedCallCount", 0),
                        "validity", Map.of("annotationCoverage", 1.0, "jsonValidRate", 1.0,
                                "evidenceValidRate", 1.0, "callSuccessRate", 1.0),
                        "usage", Map.of("totalTokens", 80, "estimatedTokenRowCount", 0, "averageLatencyMs", 20.0),
                        "rule", Map.of("sentimentAccuracy", 1.0, "problemMicroF1", 1.0, "problemMacroF1", 1.0),
                        "ai", Map.of("sentimentAccuracy", 0.0, "problemMicroF1", 0.0, "problemMacroF1", 0.0),
                        "delta", Map.of("sentimentAccuracy", -1.0, "problemMicroF1", -1.0, "problemMacroF1", -1.0)
                )
        ));

        CommentAiEvaluationVO result = service.evaluateRun(7L);

        assertThat(result.getQualityReady()).isTrue();
        assertThat(result.getRule().getSentimentAccuracy()).isEqualTo(1.0);
        assertThat(result.getAi().getProblemMicroF1()).isZero();
        assertThat(result.getDelta().getSentimentAccuracy()).isEqualTo(-1.0);
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAnalysisClient).evaluateCommentShadow(requestCaptor.capture());
        assertThat(requestCaptor.getValue().get("rows")).isInstanceOf(List.class);
    }

    @Test
    void annotationUpsertPersistsOnlyReusableAnnotationData() {
        when(annotationMapper.selectOne(any())).thenReturn(null);
        CommentAiAnnotationDTO dto = new CommentAiAnnotationDTO();
        dto.setManualSentiment("negative");
        dto.setManualProblemTypes(List.of("delivery"));
        dto.setAnnotationNote("Late delivery");

        service.upsertAnnotation(31L, dto);

        ArgumentCaptor<BizCommentAiAnnotation> captor = ArgumentCaptor.forClass(BizCommentAiAnnotation.class);
        verify(annotationMapper).insert(captor.capture());
        assertThat(captor.getValue().getCommentId()).isEqualTo(31L);
        assertThat(captor.getValue().getManualProblemTypes()).contains("delivery");
    }

    private BizCommentAiShadowRun run() {
        BizCommentAiShadowRun run = new BizCommentAiShadowRun();
        run.setId(7L);
        run.setRunStatus("success");
        return run;
    }

    private Map<String, Object> row(String manualSentiment) {
        return Map.ofEntries(
                Map.entry("commentId", 31L),
                Map.entry("ruleSentiment", "negative"),
                Map.entry("ruleProblemType", "delivery"),
                Map.entry("aiSentiment", "negative"),
                Map.entry("aiProblems", "[\"delivery\"]"),
                Map.entry("callStatus", "success"),
                Map.entry("jsonValid", 1),
                Map.entry("evidenceValid", 1),
                Map.entry("tokenUsage", 80),
                Map.entry("tokenUsageEstimated", 0),
                Map.entry("latencyMs", 20L),
                Map.entry("manualSentiment", manualSentiment == null ? "" : manualSentiment),
                Map.entry("manualProblemTypes", manualSentiment == null ? "[]" : "[\"delivery\"]")
        );
    }

    private Map<String, Object> failedRow() {
        return Map.ofEntries(
                Map.entry("commentId", 31L),
                Map.entry("ruleSentiment", "negative"),
                Map.entry("ruleProblemType", "delivery"),
                Map.entry("aiSentiment", ""),
                Map.entry("aiProblems", "[]"),
                Map.entry("callStatus", "failed"),
                Map.entry("jsonValid", 0),
                Map.entry("evidenceValid", 0),
                Map.entry("tokenUsage", 0),
                Map.entry("tokenUsageEstimated", 0),
                Map.entry("latencyMs", 15L),
                Map.entry("manualSentiment", ""),
                Map.entry("manualProblemTypes", "[]")
        );
    }
}
