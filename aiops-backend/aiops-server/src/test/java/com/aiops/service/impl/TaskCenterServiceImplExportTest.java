package com.aiops.service.impl;

import com.aiops.context.BaseContext;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCrawlTaskMapper;
import com.aiops.mapper.BizSyncConfigMapper;
import com.aiops.mapper.BizSyncExecutionMapper;
import com.aiops.service.AnalysisService;
import com.aiops.service.AiJobService;
import com.aiops.service.CommentAiShadowService;
import com.aiops.service.DataImportService;
import com.aiops.service.SyncConfigService;
import com.aiops.vo.AiJobCreatedVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskCenterServiceImplExportTest {

    @Mock
    private BizAnalysisTaskMapper analysisTaskMapper;

    @Mock
    private BizCrawlTaskMapper crawlTaskMapper;

    @Mock
    private BizSyncExecutionMapper syncExecutionMapper;

    @Mock
    private BizSyncConfigMapper syncConfigMapper;

    @Mock
    private DataImportService dataImportService;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private CommentAiShadowService commentAiShadowService;

    @Mock
    private SyncConfigService syncConfigService;

    @Mock
    private AiJobService aiJobService;

    private TaskCenterServiceImpl taskCenterService;

    @BeforeEach
    void setUp() {
        taskCenterService = new TaskCenterServiceImpl(
                analysisTaskMapper,
                crawlTaskMapper,
                syncExecutionMapper,
                syncConfigMapper,
                dataImportService,
                analysisService,
                commentAiShadowService,
                syncConfigService,
                aiJobService,
                new ObjectMapper()
        );
    }

    @Test
    void exportTasksCsvUsesCurrentFilters() {
        BizAnalysisTask analysisTask = new BizAnalysisTask();
        analysisTask.setId(7L);
        analysisTask.setTaskType("comment_analysis");
        analysisTask.setTaskStatus("success");
        analysisTask.setTargetType("product");
        analysisTask.setTargetId("product-1");
        analysisTask.setProgress(100);
        analysisTask.setCreateTime(LocalDateTime.of(2026, 8, 24, 10, 0));
        when(analysisTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(analysisTask));
        when(crawlTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(syncExecutionMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        byte[] csv = taskCenterService.exportTasksCsv("comment_analysis", "success", "product-1");
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(content).contains("recordKey,taskName,taskType,taskStatus,progress,targetType,targetId");
        assertThat(content).contains("analysis:7");
        assertThat(content).contains("product-1");
    }

    @Test
    void excludesOwnerBackedAiTasksFromAnotherUser() {
        BizAnalysisTask foreignTask = new BizAnalysisTask();
        foreignTask.setId(8L);
        foreignTask.setUserId(99L);
        foreignTask.setTaskType("operation_report");
        foreignTask.setTaskStatus("pending");
        foreignTask.setTargetType("product");
        foreignTask.setTargetId("product-2");
        foreignTask.setCreateTime(LocalDateTime.now());
        when(analysisTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(foreignTask));
        when(crawlTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(syncExecutionMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        BaseContext.setCurrentId(9L);

        var page = taskCenterService.pageTasks(1, 10, null, null, null);

        assertThat(page.getRecords()).isEmpty();
        assertThatThrownBy(() -> taskCenterService.getTask("analysis:8"))
                .hasMessageContaining("不存在");
        BaseContext.removeCurrentId();
    }

    @Test
    void retriesAiJobsThroughTheDurableAiJobService() {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(7L);
        task.setUserId(9L);
        task.setTaskType("operation_report");
        task.setTaskStatus("failed");
        when(analysisTaskMapper.selectById(7L)).thenReturn(task);
        when(aiJobService.retryOwnedJob(7L)).thenReturn(new AiJobCreatedVO(8L, "pending", false));
        BaseContext.setCurrentId(9L);

        var retry = taskCenterService.retryTask("analysis:7");

        assertThat(retry.getTaskId()).isEqualTo(8L);
        assertThat(retry.getImportType()).isEqualTo("operation_report");
        verify(aiJobService).retryOwnedJob(7L);
        BaseContext.removeCurrentId();
    }
}
