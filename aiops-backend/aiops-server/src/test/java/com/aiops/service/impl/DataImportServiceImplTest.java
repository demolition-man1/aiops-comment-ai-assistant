package com.aiops.service.impl;

import com.aiops.client.PythonAnalysisClient;
import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.dto.CsvImportPreflightDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCrawlTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCrawlTaskMapper;
import com.aiops.service.CacheService;
import com.aiops.service.FileService;
import com.aiops.vo.FileSignedUrlVO;
import com.aiops.vo.CsvImportPreflightVO;
import com.aiops.vo.TaskVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataImportServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper analysisTaskMapper;

    @Mock
    private BizCrawlTaskMapper crawlTaskMapper;

    @Mock
    private PythonAnalysisClient pythonAnalysisClient;

    @Mock
    private CacheService cacheService;

    @Mock
    private FileService fileService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<Runnable> backgroundTask = new AtomicReference<>();
    private DataImportServiceImpl dataImportService;

    @BeforeEach
    void setUp() {
        TaskExecutor taskExecutor = backgroundTask::set;
        dataImportService = new DataImportServiceImpl(
                analysisTaskMapper,
                crawlTaskMapper,
                pythonAnalysisClient,
                cacheService,
                fileService,
                objectMapper,
                taskExecutor
        );
    }

    @Test
    void importCsvRejectsEmptySourceBeforeCreatingTask() {
        CsvImportDTO csvImportDTO = new CsvImportDTO();

        assertThatThrownBy(() -> dataImportService.importCsv(csvImportDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本地 Olist 数据目录不能为空");

        verify(analysisTaskMapper, never()).insert(any(BizAnalysisTask.class));
        verify(pythonAnalysisClient, never()).importCsv(any());
    }

    @Test
    void importCsvStoresRequestParamAsValidJson() throws Exception {
        when(analysisTaskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            BizAnalysisTask task = invocation.getArgument(0);
            task.setId(42L);
            return 1;
        });

        CsvImportDTO csvImportDTO = new CsvImportDTO();
        csvImportDTO.setDataPath("D:\\666\\olist-brazilian-ecommerce");
        csvImportDTO.setDataSource("olist");
        csvImportDTO.setImportMode("full");

        dataImportService.importCsv(csvImportDTO);

        ArgumentCaptor<BizAnalysisTask> taskCaptor = ArgumentCaptor.forClass(BizAnalysisTask.class);
        verify(analysisTaskMapper).insert(taskCaptor.capture());
        JsonNode requestParam = objectMapper.readTree(taskCaptor.getValue().getRequestParam());

        assertThat(requestParam.get("dataPath").asText()).isEqualTo("D:\\666\\olist-brazilian-ecommerce");
        assertThat(requestParam.get("dataSource").asText()).isEqualTo("olist");
        assertThat(requestParam.get("importMode").asText()).isEqualTo("full");
    }

    @Test
    void preflightCsvReportsDuplicateByFileHashBeforeImport() {
        BizAnalysisTask existingTask = new BizAnalysisTask();
        existingTask.setId(77L);
        existingTask.setTaskStatus("success");
        existingTask.setRequestParam("{\"fileHash\":\"abc123\",\"dataSource\":\"platform_csv\"}");
        when(analysisTaskMapper.selectList(any())).thenReturn(List.of(existingTask));

        CsvImportPreflightDTO preflightDTO = new CsvImportPreflightDTO();
        preflightDTO.setFileName("reviews.csv");
        preflightDTO.setFileHash("abc123");
        preflightDTO.setEstimatedRows(12L);

        CsvImportPreflightVO result = dataImportService.preflightCsv(preflightDTO);

        assertThat(result.getReady()).isTrue();
        assertThat(result.getEstimatedRows()).isEqualTo(12L);
        assertThat(result.getDuplicateLikely()).isTrue();
        assertThat(result.getLastTaskId()).isEqualTo(77L);
        assertThat(result.getRequiredFields()).containsExactly("product_id", "review_score");
    }

    @Test
    void importSampleCreatesCsvTaskWithSampleDataFlag() throws Exception {
        when(analysisTaskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            BizAnalysisTask task = invocation.getArgument(0);
            task.setId(91L);
            return 1;
        });

        TaskVO result = dataImportService.importSample();

        assertThat(result.getTaskId()).isEqualTo(91L);
        assertThat(result.getTaskStatus()).isEqualTo("processing");
        assertThat(backgroundTask.get()).isNotNull();

        ArgumentCaptor<BizAnalysisTask> taskCaptor = ArgumentCaptor.forClass(BizAnalysisTask.class);
        verify(analysisTaskMapper).insert(taskCaptor.capture());
        JsonNode requestParam = objectMapper.readTree(taskCaptor.getValue().getRequestParam());
        assertThat(requestParam.get("sampleData").asBoolean()).isTrue();
        assertThat(requestParam.get("dataSource").asText()).isEqualTo("sample");
        assertThat(requestParam.get("importMode").asText()).isEqualTo("incremental");

        when(pythonAnalysisClient.importCsv(any())).thenReturn(Map.of("success", true));
        backgroundTask.get().run();
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAnalysisClient).importCsv(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).containsEntry("sampleData", true);
    }

    @Test
    void importCsvReturnsProcessingBeforeBackgroundImportCompletes() {
        when(analysisTaskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            BizAnalysisTask task = invocation.getArgument(0);
            task.setId(42L);
            return 1;
        });

        CsvImportDTO csvImportDTO = new CsvImportDTO();
        csvImportDTO.setDataPath("D:\\666\\olist-brazilian-ecommerce");
        csvImportDTO.setDataSource("olist");
        csvImportDTO.setImportMode("full");

        TaskVO result = dataImportService.importCsv(csvImportDTO);

        assertThat(result.getTaskId()).isEqualTo(42L);
        assertThat(result.getTaskStatus()).isEqualTo("processing");
        assertThat(result.getProgress()).isZero();
        assertThat(backgroundTask.get()).isNotNull();
        verify(pythonAnalysisClient, never()).importCsv(any());

        when(pythonAnalysisClient.importCsv(any())).thenReturn(Map.of("success", true));
        backgroundTask.get().run();
        verify(pythonAnalysisClient).importCsv(any());
    }

    @Test
    void importCsvUsesSignedUrlWhenOnlyFileIdIsProvided() {
        when(analysisTaskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            BizAnalysisTask task = invocation.getArgument(0);
            task.setId(88L);
            return 1;
        });
        when(fileService.signedUrl(15L)).thenReturn(new FileSignedUrlVO(
                15L,
                "aiops/csv/comment.csv",
                "https://signed.example.com/comment.csv",
                3600
        ));
        when(pythonAnalysisClient.importCsv(any())).thenReturn(Map.of("success", true));

        CsvImportDTO csvImportDTO = new CsvImportDTO();
        csvImportDTO.setFileId(15L);
        csvImportDTO.setDataSource("platform_csv");
        csvImportDTO.setImportMode("incremental");

        dataImportService.importCsv(csvImportDTO);
        backgroundTask.get().run();

        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAnalysisClient).importCsv(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).containsEntry("fileUrl", "https://signed.example.com/comment.csv");
        assertThat(requestCaptor.getValue()).containsEntry("objectKey", "aiops/csv/comment.csv");
    }

    @Test
    void importByCrawlerKeepsFailedStatusWhenPythonReturnsFailure() {
        when(crawlTaskMapper.insert(any(BizCrawlTask.class))).thenAnswer(invocation -> {
            BizCrawlTask task = invocation.getArgument(0);
            task.setId(9L);
            return 1;
        });
        when(pythonAnalysisClient.importByCrawler(any())).thenReturn(Map.of(
                "success", false,
                "message", "crawler adapter is not implemented"
        ));

        CrawlerImportDTO crawlerImportDTO = new CrawlerImportDTO();
        crawlerImportDTO.setPlatform("demo");
        crawlerImportDTO.setTargetUrl("https://example.com/product/1");

        TaskVO result = dataImportService.importByCrawler(crawlerImportDTO);

        assertThat(result.getTaskStatus()).isEqualTo("failed");
        assertThat(result.getProgress()).isEqualTo(100);
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getErrorMessage()).contains("crawler adapter is not implemented");
    }

    @Test
    void getImportTaskUsesCachedImportTypeWhenTaskIdsOverlap() {
        BizCrawlTask crawlTask = new BizCrawlTask();
        crawlTask.setId(5L);
        crawlTask.setTaskStatus("processing");
        crawlTask.setProgress(60);
        crawlTask.setSuccessCount(3);
        crawlTask.setFailCount(1);

        when(cacheService.get("task:type:5", String.class))
                .thenReturn(Optional.of("crawler"));
        when(crawlTaskMapper.selectById(5L)).thenReturn(crawlTask);

        TaskVO result = dataImportService.getImportTask(5L);

        assertThat(result.getImportType()).isEqualTo("crawler");
        assertThat(result.getTaskStatus()).isEqualTo("processing");
        assertThat(result.getProgress()).isEqualTo(60);
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailCount()).isEqualTo(1);
        verify(cacheService).set("task:type:5", "crawler", Duration.ofHours(2));
        verify(analysisTaskMapper, never()).selectById(5L);
    }
}
