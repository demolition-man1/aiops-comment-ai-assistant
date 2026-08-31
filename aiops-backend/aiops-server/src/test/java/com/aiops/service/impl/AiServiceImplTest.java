package com.aiops.service.impl;

import com.aiops.client.PythonAiClient;
import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.dto.CommentTranslateDTO;
import com.aiops.dto.NegativeReplyEffectDTO;
import com.aiops.dto.NegativeReplyFavoriteDTO;
import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.entity.BizAiContentRecord;
import com.aiops.entity.BizComment;
import com.aiops.entity.BizCommentAnalysisResult;
import com.aiops.entity.BizNegativeReply;
import com.aiops.entity.BizOperationReport;
import com.aiops.entity.BizOperationReportEvidence;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiContentRecordMapper;
import com.aiops.mapper.BizCommentAnalysisResultMapper;
import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizNegativeReplyMapper;
import com.aiops.mapper.BizOperationReportMapper;
import com.aiops.mapper.BizOperationReportEvidenceMapper;
import com.aiops.vo.ReportEvidenceVO;
import com.aiops.service.AiCallLogService;
import com.aiops.service.AiRateLimitService;
import com.aiops.service.CacheService;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.NegativeReplyVO;
import com.aiops.vo.CommentTranslationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private PythonAiClient pythonAiClient;

    @Mock
    private BizCommentAnalysisResultMapper analysisResultMapper;

    @Mock
    private BizOperationReportMapper operationReportMapper;

    @Mock
    private BizOperationReportEvidenceMapper operationReportEvidenceMapper;

    @Mock
    private BizAiContentRecordMapper aiContentRecordMapper;

    @Mock
    private BizCommentMapper commentMapper;

    @Mock
    private BizNegativeReplyMapper negativeReplyMapper;

    @Mock
    private CacheService cacheService;

    @Mock
    private AiRateLimitService aiRateLimitService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private AiCallLogService aiCallLogService;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(
                pythonAiClient,
                analysisResultMapper,
                operationReportMapper,
                operationReportEvidenceMapper,
                aiContentRecordMapper,
                commentMapper,
                negativeReplyMapper,
                cacheService,
                aiRateLimitService,
                promptTemplateService,
                aiCallLogService,
                new ObjectMapper()
        );
    }

    @Test
    void generateContentSendsLanguageToPythonAndStoresRecord() {
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(cacheService.get(anyString(), eq(com.aiops.vo.AiContentVO.class))).thenReturn(Optional.empty());
        when(pythonAiClient.generateContent(any())).thenReturn(Map.of(
                "success", true,
                "generatedContent", "English product copy",
                "modelName", "deepseek-chat"
        ));
        when(aiContentRecordMapper.insert(any(BizAiContentRecord.class))).thenAnswer(invocation -> {
            BizAiContentRecord record = invocation.getArgument(0);
            record.setId(31L);
            return 1;
        });

        AiContentGenerateDTO generateDTO = new AiContentGenerateDTO();
        generateDTO.setTargetType("product");
        generateDTO.setTargetId("product-a");
        generateDTO.setContentType("product_title");
        generateDTO.setStyleType("simple");
        generateDTO.setLanguage("en-US");

        aiService.generateContent(generateDTO);

        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAiClient).generateContent(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).containsEntry("language", "en-US");

        ArgumentCaptor<BizAiContentRecord> recordCaptor = ArgumentCaptor.forClass(BizAiContentRecord.class);
        verify(aiContentRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getGeneratedContent()).isEqualTo("English product copy");
        verify(aiCallLogService).record(any(), eq("content"), eq("product"), eq("product-a"),
                any(), eq("deepseek-chat"), eq("success"), any(), any(), any());
    }

    @Test
    void generateProductReportUsesLanguageInCacheKey() {
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(cacheService.get(eq("ai:report:product:product-a:en-US"), eq(com.aiops.vo.OperationReportVO.class)))
                .thenReturn(Optional.empty());
        when(analysisResultMapper.selectOne(any())).thenReturn(analysisResult());
        when(pythonAiClient.generateReport(any())).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "reportTitle", "English report",
                        "consumerPainPoints", "Slow delivery",
                        "productAdvantages", "Good quality",
                        "productDisadvantages", "Logistics risk",
                        "operationSuggestions", "Improve shipping",
                        "copywritingSuggestions", "Emphasize delivery SLA",
                        "serviceSuggestions", "Proactive notice",
                        "fullReport", "English report body",
                        "modelName", "deepseek-chat"
                )
        ));
        when(operationReportMapper.insert(any(BizOperationReport.class))).thenAnswer(invocation -> {
            BizOperationReport report = invocation.getArgument(0);
            report.setId(41L);
            return 1;
        });

        com.aiops.dto.AiReportGenerateDTO generateDTO = new com.aiops.dto.AiReportGenerateDTO();
        generateDTO.setProductId("product-a");
        generateDTO.setLanguage("en-US");

        aiService.generateProductReport(generateDTO);

        verify(cacheService).get(eq("ai:report:product:product-a:en-US"), eq(com.aiops.vo.OperationReportVO.class));
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAiClient).generateReport(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).containsEntry("language", "en-US");
        verify(aiCallLogService).record(any(), eq("report"), eq("product"), eq("product-a"),
                any(), eq("deepseek-chat"), eq("success"), any(), any(), any());
    }

    @Test
    void generateProductReportForceRefreshBypassesCachedReport() {
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(analysisResultMapper.selectOne(any())).thenReturn(analysisResult());
        when(pythonAiClient.generateReport(any())).thenReturn(Map.of(
                "success", true,
                "data", Map.of("reportTitle", "Fresh report", "modelName", "deepseek-chat")
        ));
        when(operationReportMapper.insert(any(BizOperationReport.class))).thenAnswer(invocation -> {
            BizOperationReport report = invocation.getArgument(0);
            report.setId(42L);
            return 1;
        });

        com.aiops.dto.AiReportGenerateDTO generateDTO = new com.aiops.dto.AiReportGenerateDTO();
        generateDTO.setProductId("product-a");
        generateDTO.setLanguage("zh-CN");
        generateDTO.setForceRefresh(true);

        aiService.generateProductReport(generateDTO);

        verify(cacheService, never()).get(eq("ai:report:product:product-a:zh-CN"), eq(com.aiops.vo.OperationReportVO.class));
        verify(pythonAiClient).generateReport(any());
    }

    @Test
    void generateNegativeReplySendsFullCommentContextToPython() {
        BizComment comment = new BizComment();
        comment.setId(22L);
        comment.setReviewId("review-22");
        comment.setProductId("product-a");
        comment.setSellerId("seller-a");
        comment.setReviewScore(2);
        comment.setReviewTitle("Entrega atrasada");
        comment.setReviewContent("Produto chegou quebrado");
        comment.setCleanContent("produto chegou quebrado");
        comment.setProblemType("quality");
        comment.setManualProblemType("packaging");
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(commentMapper.selectById(22L)).thenReturn(comment);
        when(pythonAiClient.generateNegativeReply(any())).thenReturn(Map.of(
                "success", true,
                "replyContent", "专门针对包装破损的回复",
                "modelName", "deepseek-chat"
        ));

        NegativeReplyGenerateDTO generateDTO = new NegativeReplyGenerateDTO();
        generateDTO.setCommentId(22L);
        generateDTO.setToneType("professional");
        generateDTO.setLanguage("zh-CN");

        aiService.generateNegativeReply(generateDTO);

        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAiClient).generateNegativeReply(requestCaptor.capture());
        Map<String, Object> request = requestCaptor.getValue();
        assertThat(request)
                .containsEntry("commentId", 22L)
                .containsEntry("reviewId", "review-22")
                .containsEntry("productId", "product-a")
                .containsEntry("sellerId", "seller-a")
                .containsEntry("reviewScore", 2)
                .containsEntry("commentTitle", "Entrega atrasada")
                .containsEntry("commentContent", "produto chegou quebrado")
                .containsEntry("problemType", "packaging");
    }

    @Test
    void generateProductReportPersistsValidatedEvidenceReferences() {
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(cacheService.get(eq("ai:report:product:product-a:en-US"), eq(com.aiops.vo.OperationReportVO.class)))
                .thenReturn(Optional.empty());
        when(analysisResultMapper.selectOne(any())).thenReturn(analysisResult());
        when(pythonAiClient.generateReport(any())).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "reportTitle", "Evidence report",
                        "modelName", "deepseek-chat",
                        "references", List.of(
                                Map.of("sourceType", "review_evidence", "sourceId", 31, "title", "Review #31", "score", 0.91),
                                Map.of("sourceType", "problem_solution", "sourceId", 8, "title", "Delivery guide", "score", 0.88),
                                Map.of("sourceType", "historical_reply", "sourceId", 6, "title", "Ignore", "score", 0.90)
                        )
                )
        ));
        when(operationReportMapper.insert(any(BizOperationReport.class))).thenAnswer(invocation -> {
            invocation.<BizOperationReport>getArgument(0).setId(51L);
            return 1;
        });
        BizOperationReportEvidence reviewEvidence = reportEvidence(
                "review_evidence", 31L, "Review #31", 0.91);
        BizOperationReportEvidence solutionEvidence = reportEvidence(
                "problem_solution", 8L, "Delivery guide", 0.88);
        when(operationReportEvidenceMapper.selectByReportId(51L)).thenReturn(List.of(reviewEvidence, solutionEvidence));

        com.aiops.dto.AiReportGenerateDTO generateDTO = new com.aiops.dto.AiReportGenerateDTO();
        generateDTO.setProductId("product-a");
        generateDTO.setLanguage("en-US");

        var result = aiService.generateProductReport(generateDTO);

        assertThat(result.getEvidence()).extracting(ReportEvidenceVO::getSourceType)
                .containsExactly("review_evidence", "problem_solution");
        verify(operationReportEvidenceMapper, org.mockito.Mockito.times(2)).insert(any(BizOperationReportEvidence.class));
    }

    private BizOperationReportEvidence reportEvidence(String sourceType, Long sourceId, String title, double score) {
        BizOperationReportEvidence evidence = new BizOperationReportEvidence();
        evidence.setSourceType(sourceType);
        evidence.setSourceId(sourceId);
        evidence.setSourceTitle(title);
        evidence.setRelevanceScore(score);
        evidence.setRetrievalVersion("review-evidence-v1");
        return evidence;
    }

    @Test
    void generateNegativeReplyPersistsOnlyValidRagReferences() {
        BizComment comment = new BizComment();
        comment.setId(22L);
        comment.setProductId("product-a");
        comment.setSellerId("seller-a");
        comment.setReviewScore(1);
        comment.setReviewContent("Delivery never arrived");
        comment.setCleanContent("delivery never arrived");
        comment.setProblemType("logistics");
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(commentMapper.selectById(22L)).thenReturn(comment);
        when(pythonAiClient.generateNegativeReply(any())).thenReturn(Map.of(
                "success", true,
                "replyContent", "We are checking the delivery.",
                "modelName", "deepseek-chat",
                "ragUsed", true,
                "references", List.of(
                        Map.of("sourceType", "problem_solution", "sourceId", 9, "title", "Delivery follow-up", "score", 0.91),
                        Map.of("sourceType", "unknown", "sourceId", 10, "title", "Ignore", "score", 0.8),
                        Map.of("sourceType", "historical_reply", "sourceId", "bad", "title", "Ignore", "score", 0.7)
                )
        ));
        when(negativeReplyMapper.insert(any(BizNegativeReply.class))).thenAnswer(invocation -> {
            invocation.<BizNegativeReply>getArgument(0).setId(36L);
            return 1;
        });

        NegativeReplyGenerateDTO generateDTO = new NegativeReplyGenerateDTO();
        generateDTO.setCommentId(22L);

        NegativeReplyVO result = aiService.generateNegativeReply(generateDTO);

        assertThat(result.getRagUsed()).isTrue();
        assertThat(result.getRagReferences())
                .singleElement()
                .satisfies(reference -> {
                    assertThat(reference.getSourceType()).isEqualTo("problem_solution");
                    assertThat(reference.getSourceId()).isEqualTo(9L);
                    assertThat(reference.getTitle()).isEqualTo("Delivery follow-up");
                });
        ArgumentCaptor<BizNegativeReply> replyCaptor = ArgumentCaptor.forClass(BizNegativeReply.class);
        verify(negativeReplyMapper).insert(replyCaptor.capture());
        assertThat(replyCaptor.getValue().getRagReferences()).contains("Delivery follow-up");
        assertThat(replyCaptor.getValue().getRagReferences()).doesNotContain("Ignore");
    }

    @Test
    void generateNegativeReplyRejectsMissingCommentIdBeforeCallingPython() {
        NegativeReplyGenerateDTO generateDTO = new NegativeReplyGenerateDTO();

        assertThatThrownBy(() -> aiService.generateNegativeReply(generateDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("评论 ID 不能为空");

        verify(aiRateLimitService, never()).tryConsume(anyString(), any());
        verify(pythonAiClient, never()).generateNegativeReply(any());
    }

    @Test
    void translateCommentUsesTargetLanguageCacheAndSendsReviewTextToPython() {
        BizComment comment = new BizComment();
        comment.setId(22L);
        comment.setReviewId("review-22");
        comment.setProductId("product-a");
        comment.setSellerId("seller-a");
        comment.setReviewScore(2);
        comment.setReviewTitle("Entrega atrasada");
        comment.setReviewContent("Produto chegou quebrado");
        comment.setCleanContent("produto chegou quebrado");
        when(commentMapper.selectById(22L)).thenReturn(comment);
        when(cacheService.get(eq("ai:translation:comment:22:en-US"), eq(CommentTranslationVO.class)))
                .thenReturn(Optional.empty());
        when(aiRateLimitService.tryConsume(anyString(), any())).thenReturn(true);
        when(pythonAiClient.translateComment(any())).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "translatedContent", "The product arrived broken.",
                        "sourceLanguage", "pt-BR",
                        "modelName", "deepseek-chat"
                )
        ));

        CommentTranslateDTO translateDTO = new CommentTranslateDTO();
        translateDTO.setLanguage("en-US");

        CommentTranslationVO result = aiService.translateComment(22L, translateDTO);

        assertThat(result.getCommentId()).isEqualTo(22L);
        assertThat(result.getTargetLanguage()).isEqualTo("en-US");
        assertThat(result.getTranslatedContent()).isEqualTo("The product arrived broken.");
        assertThat(result.getCached()).isFalse();
        verify(cacheService).get(eq("ai:translation:comment:22:en-US"), eq(CommentTranslationVO.class));
        verify(cacheService).set(eq("ai:translation:comment:22:en-US"), any(CommentTranslationVO.class), any());
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonAiClient).translateComment(requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .containsEntry("commentId", 22L)
                .containsEntry("reviewId", "review-22")
                .containsEntry("targetLanguage", "en-US")
                .containsEntry("commentContent", "produto chegou quebrado");
    }

    @Test
    void translateCommentReturnsCachedResultWithoutAiRateLimit() {
        CommentTranslationVO cached = new CommentTranslationVO(
                22L,
                "product-a",
                "review text",
                "pt-BR",
                "zh-CN",
                "评论文本",
                "deepseek-chat",
                false
        );
        when(commentMapper.selectById(22L)).thenReturn(new BizComment());
        when(cacheService.get(eq("ai:translation:comment:22:zh-CN"), eq(CommentTranslationVO.class)))
                .thenReturn(Optional.of(cached));

        CommentTranslationVO result = aiService.translateComment(22L, new CommentTranslateDTO());

        assertThat(result.getCached()).isTrue();
        verify(aiRateLimitService, never()).tryConsume(anyString(), any());
        verify(pythonAiClient, never()).translateComment(any());
    }

    @Test
    void markNegativeReplyUsedIncrementsUseCount() {
        BizNegativeReply reply = reply();
        reply.setUseCount(2);
        when(negativeReplyMapper.selectById(7L)).thenReturn(reply);

        NegativeReplyVO result = aiService.markNegativeReplyUsed(7L);

        assertThat(result.getUseCount()).isEqualTo(3);
        ArgumentCaptor<BizNegativeReply> captor = ArgumentCaptor.forClass(BizNegativeReply.class);
        verify(negativeReplyMapper).updateById(captor.capture());
        assertThat(captor.getValue().getUseCount()).isEqualTo(3);
        assertThat(captor.getValue().getUpdateTime()).isNotNull();
    }

    @Test
    void updateNegativeReplyEffectAndFavoriteReturnsExpandedView() {
        BizNegativeReply reply = reply();
        when(negativeReplyMapper.selectById(7L)).thenReturn(reply);

        NegativeReplyEffectDTO effectDTO = new NegativeReplyEffectDTO();
        effectDTO.setEffectTag("resolved");
        NegativeReplyVO effectResult = aiService.updateNegativeReplyEffect(7L, effectDTO);

        assertThat(effectResult.getEffectTag()).isEqualTo("resolved");

        NegativeReplyFavoriteDTO favoriteDTO = new NegativeReplyFavoriteDTO();
        favoriteDTO.setFavoriteFlag(1);
        NegativeReplyVO favoriteResult = aiService.updateNegativeReplyFavorite(7L, favoriteDTO);

        assertThat(favoriteResult.getFavoriteFlag()).isEqualTo(1);
        verify(negativeReplyMapper, org.mockito.Mockito.times(2)).updateById(org.mockito.Mockito.any(BizNegativeReply.class));
    }

    private BizNegativeReply reply() {
        BizNegativeReply reply = new BizNegativeReply();
        reply.setId(7L);
        reply.setCommentId(10L);
        reply.setProductId("product-a");
        reply.setSellerId("seller-a");
        reply.setProblemType("logistics");
        reply.setToneType("sincere");
        reply.setReplyContent("Sorry for the inconvenience.");
        reply.setModelName("deepseek-chat");
        reply.setUseCount(0);
        reply.setFavoriteFlag(0);
        return reply;
    }

    private BizCommentAnalysisResult analysisResult() {
        BizCommentAnalysisResult result = new BizCommentAnalysisResult();
        result.setId(5L);
        result.setTaskId(3L);
        result.setTargetType("product");
        result.setTargetId("product-a");
        result.setTotalCount(10);
        result.setPositiveCount(7);
        result.setNeutralCount(1);
        result.setNegativeCount(2);
        result.setPositiveRate(new BigDecimal("70.00"));
        result.setNegativeRate(new BigDecimal("20.00"));
        result.setSummary("analysis summary");
        result.setCreateTime(LocalDateTime.now());
        return result;
    }
}
