package com.aiops.service.impl;

import com.aiops.client.PythonAnalysisClient;
import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCommentAiShadowRun;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCommentAiAnnotationMapper;
import com.aiops.mapper.BizCommentAiDecisionMapper;
import com.aiops.mapper.BizCommentAiShadowResultMapper;
import com.aiops.mapper.BizCommentAiShadowRunMapper;
import com.aiops.service.AiCallLogService;
import com.aiops.service.PromptTemplateService;
import com.aiops.properties.CommentAiHybridProperties;
import com.aiops.vo.CommentAiShadowTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAiShadowServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private BizCommentAiShadowRunMapper runMapper;

    @Mock
    private BizCommentAiShadowResultMapper resultMapper;

    @Mock
    private BizCommentAiAnnotationMapper annotationMapper;

    @Mock
    private BizCommentAiDecisionMapper decisionMapper;

    @Mock
    private PythonAnalysisClient pythonAnalysisClient;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private AiCallLogService aiCallLogService;

    private final AtomicReference<Runnable> backgroundTask = new AtomicReference<>();
    private CommentAiShadowServiceImpl service;

    @BeforeEach
    void setUp() {
        TaskExecutor taskExecutor = backgroundTask::set;
        service = new CommentAiShadowServiceImpl(
                taskMapper,
                runMapper,
                resultMapper,
                annotationMapper,
                decisionMapper,
                pythonAnalysisClient,
                promptTemplateService,
                aiCallLogService,
                new ObjectMapper(),
                taskExecutor,
                new CommentAiHybridProperties()
        );
    }

    @Test
    void createTaskReturnsBeforeCallingPythonAndRecordsAggregateUsageAfterCompletion() {
        when(taskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            BizAnalysisTask task = invocation.getArgument(0);
            task.setId(41L);
            return 1;
        });
        when(runMapper.insert(any(BizCommentAiShadowRun.class))).thenAnswer(invocation -> {
            BizCommentAiShadowRun run = invocation.getArgument(0);
            run.setId(77L);
            return 1;
        });
        when(promptTemplateService.findDefaultTemplate(eq("comment_analysis_shadow"), eq("en-US")))
                .thenReturn(Optional.of(template()));

        CommentAiShadowTaskVO created = service.createTask(request());

        assertThat(created.getTaskId()).isEqualTo(41L);
        assertThat(created.getRunId()).isEqualTo(77L);
        assertThat(created.getTaskStatus()).isEqualTo("processing");
        assertThat(backgroundTask.get()).isNotNull();
        verify(pythonAnalysisClient, never()).analyzeCommentShadow(any());

        when(pythonAnalysisClient.analyzeCommentShadow(any())).thenReturn(Map.of(
                "success", true,
                "runId", 77,
                "status", "partial",
                "actualSampleSize", 2,
                "successCount", 1,
                "failureCount", 1,
                "totalCalls", 2,
                "totalTokens", 1400,
                "modelName", "deepseek-chat"
        ));
        backgroundTask.get().run();

        ArgumentCaptor<BizAnalysisTask> updatedTask = ArgumentCaptor.forClass(BizAnalysisTask.class);
        verify(taskMapper).updateById(updatedTask.capture());
        assertThat(updatedTask.getValue().getTaskStatus()).isEqualTo("partial");
        assertThat(updatedTask.getValue().getProgress()).isEqualTo(100);
        verify(aiCallLogService).record(any(), eq("comment_ai_shadow"), eq("product"), eq("product-17"),
                eq(8L), eq("deepseek-chat"), eq("success"), eq(1400), anyLong(), eq(null));
    }

    private CommentAiShadowTaskDTO request() {
        CommentAiShadowTaskDTO request = new CommentAiShadowTaskDTO();
        request.setTargetType("product");
        request.setTargetId("product-17");
        request.setSampleSize(2);
        request.setSampleSeed(20260829);
        request.setMaxTotalTokens(2000);
        request.setLanguage("en-US");
        return request;
    }

    private SysPromptTemplate template() {
        SysPromptTemplate template = new SysPromptTemplate();
        template.setId(8L);
        template.setTemplateContent("Classify {reviewScore}: {reviewText}");
        return template;
    }
}
