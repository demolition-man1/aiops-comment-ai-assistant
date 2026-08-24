package com.aiops.service.impl;

import com.aiops.client.PythonAiClient;
import com.aiops.constant.RedisKeyConstant;
import com.aiops.context.BaseContext;
import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.dto.CommentTranslateDTO;
import com.aiops.dto.NegativeReplyEffectDTO;
import com.aiops.dto.NegativeReplyFavoriteDTO;
import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.entity.BizAiContentRecord;
import com.aiops.entity.BizComment;
import com.aiops.entity.BizCommentAnalysisResult;
import com.aiops.entity.BizNegativeReply;
import com.aiops.entity.BizOperationReport;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiContentRecordMapper;
import com.aiops.mapper.BizCommentAnalysisResultMapper;
import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizNegativeReplyMapper;
import com.aiops.mapper.BizOperationReportMapper;
import com.aiops.service.AiRateLimitService;
import com.aiops.service.CacheService;
import com.aiops.result.PageResult;
import com.aiops.service.AiService;
import com.aiops.vo.AiContentVO;
import com.aiops.vo.CommentTranslationVO;
import com.aiops.vo.NegativeReplyVO;
import com.aiops.vo.OperationReportVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final PythonAiClient pythonAiClient;
    private final BizCommentAnalysisResultMapper analysisResultMapper;
    private final BizOperationReportMapper operationReportMapper;
    private final BizAiContentRecordMapper aiContentRecordMapper;
    private final BizCommentMapper commentMapper;
    private final BizNegativeReplyMapper negativeReplyMapper;
    private final CacheService cacheService;
    private final AiRateLimitService aiRateLimitService;

    @Override
    public OperationReportVO generateProductReport(AiReportGenerateDTO generateDTO) {
        validateProductReportDTO(generateDTO);
        checkAiRateLimit("report");
        return generateReport("product", generateDTO.getProductId().trim(), languageOrDefault(generateDTO.getLanguage()),
                Boolean.TRUE.equals(generateDTO.getForceRefresh()));
    }

    @Override
    public OperationReportVO generateSellerReport(AiReportGenerateDTO generateDTO) {
        validateSellerReportDTO(generateDTO);
        checkAiRateLimit("report");
        return generateReport("seller", generateDTO.getSellerId().trim(), languageOrDefault(generateDTO.getLanguage()),
                Boolean.TRUE.equals(generateDTO.getForceRefresh()));
    }

    @Override
    public PageResult<OperationReportVO> pageReports(String targetType, String targetId, Integer pageNum, Integer pageSize) {
        Page<BizOperationReport> page = operationReportMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BizOperationReport>()
                        .eq(targetType != null, BizOperationReport::getTargetType, targetType)
                        .eq(targetId != null, BizOperationReport::getTargetId, targetId)
                        .orderByDesc(BizOperationReport::getCreateTime));
        List<OperationReportVO> records = page.getRecords().stream().map(this::toReportVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public OperationReportVO getReport(Long reportId) {
        BizOperationReport report = operationReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "AI 运营报告不存在");
        }
        return toReportVO(report);
    }

    @Override
    public AiContentVO generateContent(AiContentGenerateDTO generateDTO) {
        validateContentDTO(generateDTO);
        checkAiRateLimit("content");
        String cacheKey = String.format(RedisKeyConstant.AI_CONTENT,
                Integer.toHexString((generateDTO.getTargetType() + generateDTO.getTargetId()
                        + generateDTO.getContentType() + generateDTO.getStyleType()
                        + languageOrDefault(generateDTO.getLanguage()) + generateDTO.getExtraRequirement()).hashCode()));
        Optional<AiContentVO> cached = cacheService.get(cacheKey, AiContentVO.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        Map<String, Object> request = new HashMap<>();
        request.put("targetType", generateDTO.getTargetType());
        request.put("targetId", generateDTO.getTargetId());
        request.put("contentType", generateDTO.getContentType());
        request.put("styleType", generateDTO.getStyleType());
        request.put("language", languageOrDefault(generateDTO.getLanguage()));
        request.put("extraRequirement", generateDTO.getExtraRequirement());
        Map<String, Object> response = callPython(() -> pythonAiClient.generateContent(request));
        String generatedContent = stringValue(response, "generatedContent");
        String modelName = stringValue(response, "modelName");

        BizAiContentRecord record = new BizAiContentRecord();
        record.setTargetType(generateDTO.getTargetType());
        record.setTargetId(generateDTO.getTargetId());
        record.setContentType(generateDTO.getContentType());
        record.setStyleType(generateDTO.getStyleType());
        record.setGeneratedContent(generatedContent);
        record.setModelName(modelName);
        record.setCreateTime(LocalDateTime.now());
        aiContentRecordMapper.insert(record);
        AiContentVO vo = new AiContentVO(record.getId(), generatedContent, modelName);
        cacheService.set(cacheKey, vo, Duration.ofHours(12));
        return vo;
    }

    @Override
    public PageResult<AiContentVO> pageContents(String targetType, String targetId, String contentType, Integer pageNum, Integer pageSize) {
        Page<BizAiContentRecord> page = aiContentRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BizAiContentRecord>()
                        .eq(targetType != null, BizAiContentRecord::getTargetType, targetType)
                        .eq(targetId != null, BizAiContentRecord::getTargetId, targetId)
                        .eq(contentType != null, BizAiContentRecord::getContentType, contentType)
                        .orderByDesc(BizAiContentRecord::getCreateTime));
        List<AiContentVO> records = page.getRecords().stream()
                .map(record -> new AiContentVO(record.getId(), record.getGeneratedContent(), record.getModelName()))
                .toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public NegativeReplyVO generateNegativeReply(NegativeReplyGenerateDTO generateDTO) {
        validateNegativeReplyDTO(generateDTO);
        checkAiRateLimit("negative-reply");
        BizComment comment = commentMapper.selectById(generateDTO.getCommentId());
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        String problemType = firstNotBlank(comment.getManualProblemType(), comment.getProblemType(), "unknown");
        String commentContent = bestCommentContent(comment);
        Map<String, Object> request = new HashMap<>();
        request.put("commentId", comment.getId());
        request.put("reviewId", comment.getReviewId());
        request.put("productId", comment.getProductId());
        request.put("sellerId", comment.getSellerId());
        request.put("reviewScore", comment.getReviewScore());
        request.put("sentiment", comment.getSentiment());
        request.put("commentTitle", meaningfulText(comment.getReviewTitle()));
        request.put("commentContent", commentContent);
        request.put("problemType", problemType);
        request.put("toneType", blankToDefault(generateDTO.getToneType(), "sincere"));
        request.put("language", languageOrDefault(generateDTO.getLanguage()));
        Map<String, Object> response = callPython(() -> pythonAiClient.generateNegativeReply(request));
        String replyContent = stringValue(response, "replyContent");
        String modelName = stringValue(response, "modelName");

        BizNegativeReply reply = new BizNegativeReply();
        reply.setCommentId(comment.getId());
        reply.setProductId(comment.getProductId());
        reply.setSellerId(comment.getSellerId());
        reply.setProblemType(problemType);
        reply.setCommentContent(commentContent);
        reply.setToneType(blankToDefault(generateDTO.getToneType(), "sincere"));
        reply.setReplyContent(replyContent);
        reply.setModelName(modelName);
        reply.setUseCount(0);
        reply.setFavoriteFlag(0);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        negativeReplyMapper.insert(reply);
        return toNegativeReplyVO(reply);
    }

    @Override
    public CommentTranslationVO translateComment(Long commentId, CommentTranslateDTO translateDTO) {
        if (commentId == null) {
            throw new BusinessException(400, "评论 ID 不能为空");
        }
        String targetLanguage = languageOrDefault(translateDTO == null ? null : translateDTO.getLanguage());
        BizComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        String cacheKey = String.format(RedisKeyConstant.AI_COMMENT_TRANSLATION, commentId, targetLanguage);
        if (translateDTO == null || !Boolean.TRUE.equals(translateDTO.getForceRefresh())) {
            Optional<CommentTranslationVO> cached = cacheService.get(cacheKey, CommentTranslationVO.class);
            if (cached.isPresent()) {
                CommentTranslationVO vo = cached.get();
                vo.setCached(true);
                return vo;
            }
        }

        checkAiRateLimit("translation");
        String originalContent = bestCommentContent(comment);
        Map<String, Object> request = new HashMap<>();
        request.put("commentId", comment.getId());
        request.put("reviewId", comment.getReviewId());
        request.put("productId", comment.getProductId());
        request.put("sellerId", comment.getSellerId());
        request.put("reviewScore", comment.getReviewScore());
        request.put("commentTitle", meaningfulText(comment.getReviewTitle()));
        request.put("commentContent", originalContent);
        request.put("targetLanguage", targetLanguage);
        Map<String, Object> response = callPython(() -> pythonAiClient.translateComment(request));
        Map<String, Object> data = nestedData(response);
        CommentTranslationVO vo = new CommentTranslationVO(
                comment.getId(),
                comment.getProductId(),
                originalContent,
                blankToDefault(stringValue(data, "sourceLanguage"), "auto"),
                targetLanguage,
                blankToDefault(stringValue(data, "translatedContent"), stringValue(response, "translatedContent")),
                blankToDefault(stringValue(data, "modelName"), stringValue(response, "modelName")),
                false
        );
        cacheService.set(cacheKey, vo, Duration.ofDays(7));
        return vo;
    }

    @Override
    public PageResult<NegativeReplyVO> pageNegativeReplies(String productId, String sellerId, Integer pageNum, Integer pageSize) {
        Page<BizNegativeReply> page = negativeReplyMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BizNegativeReply>()
                        .eq(productId != null, BizNegativeReply::getProductId, productId)
                        .eq(sellerId != null, BizNegativeReply::getSellerId, sellerId)
                        .orderByDesc(BizNegativeReply::getCreateTime));
        List<NegativeReplyVO> records = page.getRecords().stream()
                .map(this::toNegativeReplyVO)
                .toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public NegativeReplyVO markNegativeReplyUsed(Long replyId) {
        BizNegativeReply reply = getNegativeReply(replyId);
        reply.setUseCount(Optional.ofNullable(reply.getUseCount()).orElse(0) + 1);
        reply.setUpdateTime(LocalDateTime.now());
        negativeReplyMapper.updateById(reply);
        return toNegativeReplyVO(reply);
    }

    @Override
    public NegativeReplyVO updateNegativeReplyEffect(Long replyId, NegativeReplyEffectDTO effectDTO) {
        BizNegativeReply reply = getNegativeReply(replyId);
        String effectTag = normalizeEffectTag(effectDTO == null ? null : effectDTO.getEffectTag());
        reply.setEffectTag(effectTag);
        reply.setUpdateTime(LocalDateTime.now());
        negativeReplyMapper.updateById(reply);
        return toNegativeReplyVO(reply);
    }

    @Override
    public NegativeReplyVO updateNegativeReplyFavorite(Long replyId, NegativeReplyFavoriteDTO favoriteDTO) {
        BizNegativeReply reply = getNegativeReply(replyId);
        Integer favoriteFlag = favoriteDTO == null ? 0 : favoriteDTO.getFavoriteFlag();
        reply.setFavoriteFlag(Integer.valueOf(1).equals(favoriteFlag) ? 1 : 0);
        reply.setUpdateTime(LocalDateTime.now());
        negativeReplyMapper.updateById(reply);
        return toNegativeReplyVO(reply);
    }

    private OperationReportVO generateReport(String targetType, String targetId, String language, boolean forceRefresh) {
        String cacheKey = String.format(RedisKeyConstant.AI_REPORT, targetType, targetId, language);
        if (!forceRefresh) {
            Optional<OperationReportVO> cached = cacheService.get(cacheKey, OperationReportVO.class);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        BizCommentAnalysisResult analysisResult = analysisResultMapper.selectOne(new LambdaQueryWrapper<BizCommentAnalysisResult>()
                .eq(BizCommentAnalysisResult::getTargetType, targetType)
                .eq(BizCommentAnalysisResult::getTargetId, targetId)
                .orderByDesc(BizCommentAnalysisResult::getCreateTime)
                .last("limit 1"));
        if (analysisResult == null) {
            throw new BusinessException(404, "请先生成评论分析结果");
        }
        Map<String, Object> request = new HashMap<>();
        request.put("targetType", targetType);
        request.put("targetId", targetId);
        request.put("analysisResult", analysisResult);
        request.put("language", language);
        Map<String, Object> response = callPython(() -> pythonAiClient.generateReport(request));
        Map<String, Object> data = nestedData(response);

        BizOperationReport report = new BizOperationReport();
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReportTitle(stringValue(data, "reportTitle"));
        report.setConsumerPainPoints(stringValue(data, "consumerPainPoints"));
        report.setProductAdvantages(stringValue(data, "productAdvantages"));
        report.setProductDisadvantages(stringValue(data, "productDisadvantages"));
        report.setOperationSuggestions(stringValue(data, "operationSuggestions"));
        report.setCopywritingSuggestions(stringValue(data, "copywritingSuggestions"));
        report.setServiceSuggestions(stringValue(data, "serviceSuggestions"));
        report.setFullReport(stringValue(data, "fullReport"));
        report.setModelName(stringValue(data, "modelName"));
        report.setCreateTime(LocalDateTime.now());
        operationReportMapper.insert(report);
        OperationReportVO vo = toReportVO(report);
        cacheService.set(cacheKey, vo, Duration.ofHours(12));
        return vo;
    }

    private OperationReportVO toReportVO(BizOperationReport report) {
        return new OperationReportVO(report.getId(), report.getReportTitle(), report.getConsumerPainPoints(),
                report.getProductAdvantages(), report.getProductDisadvantages(), report.getOperationSuggestions(),
                report.getCopywritingSuggestions(), report.getServiceSuggestions(), report.getFullReport(), report.getModelName());
    }

    private BizNegativeReply getNegativeReply(Long replyId) {
        BizNegativeReply reply = negativeReplyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException(404, "差评回复不存在");
        }
        return reply;
    }

    private NegativeReplyVO toNegativeReplyVO(BizNegativeReply reply) {
        return new NegativeReplyVO(
                reply.getId(),
                reply.getCommentId(),
                reply.getProductId(),
                reply.getSellerId(),
                reply.getProblemType(),
                reply.getCommentContent(),
                reply.getToneType(),
                reply.getReplyContent(),
                reply.getModelName(),
                reply.getEffectTag(),
                Optional.ofNullable(reply.getUseCount()).orElse(0),
                Optional.ofNullable(reply.getFavoriteFlag()).orElse(0),
                reply.getCreateTime(),
                reply.getUpdateTime()
        );
    }

    private String normalizeEffectTag(String effectTag) {
        if (effectTag == null || effectTag.isBlank()) {
            return null;
        }
        String normalized = effectTag.trim();
        if (!List.of("resolved", "unresolved", "positive_followup", "no_feedback").contains(normalized)) {
            throw new BusinessException(400, "无效的回复效果标记");
        }
        return normalized;
    }

    private Map<String, Object> callPython(PythonCall call) {
        try {
            Map<String, Object> response = call.execute();
            if (response == null || Boolean.FALSE.equals(response.get("success"))) {
                throw new BusinessException(503, "Python AI 服务返回失败");
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(503, "Python AI 服务不可用：" + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedData(Map<String, Object> response) {
        Object data = response.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : response;
    }

    private String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String bestCommentContent(BizComment comment) {
        String content = firstNotBlank(
                meaningfulText(comment.getCleanContent()),
                meaningfulText(comment.getReviewContent()),
                meaningfulText(comment.getReviewTitle())
        );
        if (content != null) {
            return content;
        }
        String problemType = firstNotBlank(comment.getManualProblemType(), comment.getProblemType(), "unknown");
        String score = comment.getReviewScore() == null ? "未知评分" : comment.getReviewScore() + "星";
        return "评论原文缺失，系统仅识别到评分：" + score + "，问题类型：" + problemType;
    }

    private String meaningfulText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        String compact = normalized.replaceAll("\\s+", "").toLowerCase();
        if (compact.isBlank() || compact.equals("nan") || compact.equals("nannan")
                || compact.equals("null") || compact.equals("none")) {
            return null;
        }
        return normalized;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void validateProductReportDTO(AiReportGenerateDTO generateDTO) {
        if (generateDTO == null) {
            throw new BusinessException(400, "报告生成参数不能为空");
        }
        if (generateDTO.getProductId() == null || generateDTO.getProductId().isBlank()) {
            throw new BusinessException(400, "商品 ID 不能为空");
        }
    }

    private void validateSellerReportDTO(AiReportGenerateDTO generateDTO) {
        if (generateDTO == null) {
            throw new BusinessException(400, "报告生成参数不能为空");
        }
        if (generateDTO.getSellerId() == null || generateDTO.getSellerId().isBlank()) {
            throw new BusinessException(400, "商家 ID 不能为空");
        }
    }

    private void validateContentDTO(AiContentGenerateDTO generateDTO) {
        if (generateDTO == null) {
            throw new BusinessException(400, "AI 文案生成参数不能为空");
        }
        if (generateDTO.getTargetType() == null || generateDTO.getTargetType().isBlank()) {
            throw new BusinessException(400, "目标类型不能为空");
        }
        if (generateDTO.getTargetId() == null || generateDTO.getTargetId().isBlank()) {
            throw new BusinessException(400, "目标 ID 不能为空");
        }
        if (generateDTO.getContentType() == null || generateDTO.getContentType().isBlank()) {
            throw new BusinessException(400, "文案类型不能为空");
        }
        generateDTO.setTargetType(generateDTO.getTargetType().trim());
        generateDTO.setTargetId(generateDTO.getTargetId().trim());
        generateDTO.setContentType(generateDTO.getContentType().trim());
        generateDTO.setStyleType(blankToDefault(generateDTO.getStyleType(), "simple"));
    }

    private void validateNegativeReplyDTO(NegativeReplyGenerateDTO generateDTO) {
        if (generateDTO == null) {
            throw new BusinessException(400, "差评回复生成参数不能为空");
        }
        if (generateDTO.getCommentId() == null) {
            throw new BusinessException(400, "评论 ID 不能为空");
        }
    }

    private String languageOrDefault(String language) {
        return blankToDefault(language, "zh-CN");
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void checkAiRateLimit(String businessType) {
        Long userId = BaseContext.getCurrentId();
        if (!aiRateLimitService.tryConsume(businessType, userId)) {
            throw new BusinessException(429, "AI 调用过于频繁，请稍后再试");
        }
    }

    private interface PythonCall {
        Map<String, Object> execute();
    }
}
