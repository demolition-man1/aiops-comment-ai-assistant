package com.aiops.service.impl;

import com.aiops.client.PythonAiClient;
import com.aiops.client.PythonAnalysisClient;
import com.aiops.converter.AnalysisJsonConverter;
import com.aiops.dto.AnalysisTaskCreateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCommentAnalysisResult;
import com.aiops.entity.BizProductCompareReport;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCommentAnalysisResultMapper;
import com.aiops.mapper.BizProductCompareReportMapper;
import com.aiops.service.AiCallLogService;
import com.aiops.service.AiRateLimitService;
import com.aiops.service.CacheService;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.ProductCompareReportVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private BizCommentAnalysisResultMapper resultMapper;

    @Mock
    private PythonAnalysisClient pythonAnalysisClient;

    @Mock
    private CacheService cacheService;

    @Mock
    private PythonAiClient pythonAiClient;

    @Mock
    private BizProductCompareReportMapper compareReportMapper;

    @Mock
    private AiRateLimitService aiRateLimitService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private AiCallLogService aiCallLogService;

    private AnalysisServiceImpl analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisServiceImpl(
                taskMapper,
                resultMapper,
                pythonAnalysisClient,
                cacheService,
                new AnalysisJsonConverter(new ObjectMapper()),
                pythonAiClient,
                compareReportMapper,
                new ObjectMapper(),
                aiRateLimitService,
                promptTemplateService,
                aiCallLogService
        );
    }

    @Test
    void createAnalysisTaskRejectsBlankTargetId() {
        AnalysisTaskCreateDTO createDTO = new AnalysisTaskCreateDTO();
        createDTO.setTargetType("product");
        createDTO.setTargetId(" ");
        createDTO.setAnalysisType("comment");

        assertThatThrownBy(() -> analysisService.createAnalysisTask(createDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标 ID 不能为空");

        verify(taskMapper, never()).insert(any(BizAnalysisTask.class));
        verify(pythonAnalysisClient, never()).analyzeComments(any());
    }

    @Test
    void createAnalysisTaskMarksFailedWhenPythonFindsNoComments() {
        when(taskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            BizAnalysisTask task = invocation.getArgument(0);
            task.setId(16L);
            return 1;
        });
        when(pythonAnalysisClient.analyzeComments(any())).thenReturn(Map.of(
                "success", true,
                "totalCount", 0,
                "message", "Comment analysis completed"
        ));
        when(cacheService.get(anyString(), eq(String.class))).thenReturn(Optional.empty());
        when(cacheService.get(anyString(), eq(Integer.class))).thenReturn(Optional.empty());

        AnalysisTaskCreateDTO createDTO = new AnalysisTaskCreateDTO();
        createDTO.setTargetType("product");
        createDTO.setTargetId(" aca2eb7d00ea1a7b ");
        createDTO.setAnalysisType("comment");

        var result = analysisService.createAnalysisTask(createDTO);

        assertThat(result.getTaskStatus()).isEqualTo("failed");
        assertThat(result.getProgress()).isEqualTo(100);
        assertThat(result.getErrorMessage()).contains("未找到可分析的评论");
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAnalysisClient).analyzeComments(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).containsEntry("targetId", "aca2eb7d00ea1a7b");
    }

    @Test
    void compareProductsBuildsReportFromLatestAnalysisResults() {
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(ProductCompareReportVO.class))).thenReturn(Optional.empty());
        when(resultMapper.selectOne(any())).thenReturn(
                analysisResult("product-a", 100, 10),
                analysisResult("product-b", 80, 24)
        );
        when(pythonAiClient.generateProductCompare(any())).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "compareSummary", "A has lower negative rate.",
                        "advantageAnalysis", "A delivery feedback is better.",
                        "riskAnalysis", "B has quality risk.",
                        "operationSuggestions", "Prefer A as benchmark.",
                        "modelName", "deepseek-chat"
                )
        ));
        when(compareReportMapper.insert(any(BizProductCompareReport.class))).thenAnswer(invocation -> {
            BizProductCompareReport report = invocation.getArgument(0);
            report.setId(99L);
            return 1;
        });

        ProductCompareDTO compareDTO = new ProductCompareDTO();
        compareDTO.setLeftProductId("product-a");
        compareDTO.setRightProductId("product-b");
        compareDTO.setLanguage("zh-CN");

        ProductCompareReportVO result = analysisService.compareProducts(compareDTO);

        assertThat(result.getReportId()).isEqualTo(99L);
        assertThat(result.getCompareSummary()).isEqualTo("A has lower negative rate.");
        assertThat(result.getMetricSnapshot()).contains("product-a").contains("product-b");

        ArgumentCaptor<BizProductCompareReport> captor = ArgumentCaptor.forClass(BizProductCompareReport.class);
        verify(compareReportMapper).insert(captor.capture());
        assertThat(captor.getValue().getLeftProductId()).isEqualTo("product-a");
        assertThat(captor.getValue().getRightProductId()).isEqualTo("product-b");
        verify(cacheService).set(anyString(), any(ProductCompareReportVO.class), any(Duration.class));
        verify(aiCallLogService).record(any(), eq("product_compare"), eq("product_pair"),
                eq("product-a:product-b"), any(), eq("deepseek-chat"), eq("success"), any(), any(), any());
    }

    @Test
    void compareProductsReturnsCachedReportWithoutAiRateLimit() {
        ProductCompareReportVO cached = new ProductCompareReportVO(
                8L,
                "product-a",
                "product-b",
                "{}",
                "{left=左侧缓存摘要, right=右侧缓存摘要}",
                "{left=左侧缓存优势, right=右侧缓存优势}",
                "",
                "{left=优化物流, right=保持安装体验}",
                "deepseek-chat",
                null
        );
        when(cacheService.get(anyString(), eq(ProductCompareReportVO.class))).thenReturn(Optional.of(cached));

        ProductCompareDTO compareDTO = new ProductCompareDTO();
        compareDTO.setLeftProductId("product-a");
        compareDTO.setRightProductId("product-b");

        ProductCompareReportVO result = analysisService.compareProducts(compareDTO);

        assertThat(result.getReportId()).isEqualTo(8L);
        assertThat(result.getCompareSummary()).contains("左侧：左侧缓存摘要")
                .contains("右侧：右侧缓存摘要")
                .doesNotContain("left=");
        assertThat(result.getOperationSuggestions()).contains("左侧：优化物流")
                .contains("右侧：保持安装体验")
                .doesNotContain("right=");
        verify(aiRateLimitService, never()).tryConsume(anyString(), any());
        verify(pythonAiClient, never()).generateProductCompare(any());
        verify(resultMapper, never()).selectOne(any());
    }

    @Test
    void getProductCompareReportCleansPersistedJavaMapStyleSections() {
        BizProductCompareReport report = new BizProductCompareReport();
        report.setId(10L);
        report.setLeftProductId("product-a");
        report.setRightProductId("product-b");
        report.setCompareSummary("{left=左侧商品评论量更大, right=右侧商品好评率更高}");
        report.setAdvantageAnalysis("{left=质量认可度较高, right=安装体验更好}");
        report.setRiskAnalysis("{left=物流投诉较多, right=价格敏感}");
        report.setOperationSuggestions("{left=优化物流, right=保持安装体验}");
        when(compareReportMapper.selectById(10L)).thenReturn(report);

        ProductCompareReportVO result = analysisService.getProductCompareReport(10L);

        assertThat(result.getCompareSummary()).contains("左侧：左侧商品评论量更大")
                .contains("右侧：右侧商品好评率更高")
                .doesNotContain("left=");
        assertThat(result.getOperationSuggestions()).contains("左侧：优化物流")
                .contains("右侧：保持安装体验")
                .doesNotContain("right=");
    }

    private BizCommentAnalysisResult analysisResult(String productId, int totalCount, int negativeCount) {
        BizCommentAnalysisResult result = new BizCommentAnalysisResult();
        result.setTargetType("product");
        result.setTargetId(productId);
        result.setTotalCount(totalCount);
        result.setPositiveCount(totalCount - negativeCount);
        result.setNeutralCount(0);
        result.setNegativeCount(negativeCount);
        result.setPositiveRate(BigDecimal.valueOf(totalCount - negativeCount)
                .divide(BigDecimal.valueOf(totalCount), 4, java.math.RoundingMode.HALF_UP));
        result.setNegativeRate(BigDecimal.valueOf(negativeCount)
                .divide(BigDecimal.valueOf(totalCount), 4, java.math.RoundingMode.HALF_UP));
        result.setTopKeywords("[{\"keyword\":\"produto\",\"count\":3}]");
        result.setNegativeKeywords("[{\"keyword\":\"entrega\",\"count\":2}]");
        result.setProblemDistribution("[{\"name\":\"logistics\",\"count\":2}]");
        result.setScoreDistribution("[{\"name\":\"1\",\"count\":2}]");
        result.setSummary("summary " + productId);
        return result;
    }
}
