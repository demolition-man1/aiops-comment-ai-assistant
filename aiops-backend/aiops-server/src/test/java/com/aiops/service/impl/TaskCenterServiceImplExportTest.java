package com.aiops.service.impl;

import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCrawlTaskMapper;
import com.aiops.mapper.BizSyncConfigMapper;
import com.aiops.mapper.BizSyncExecutionMapper;
import com.aiops.service.AnalysisService;
import com.aiops.service.CommentAiShadowService;
import com.aiops.service.DataImportService;
import com.aiops.service.SyncConfigService;
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
import static org.mockito.Mockito.when;

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
}
