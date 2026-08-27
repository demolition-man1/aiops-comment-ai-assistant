package com.aiops.service.impl;

import com.aiops.client.PythonAiClient;
import com.aiops.client.PythonAnalysisClient;
import com.aiops.constant.RedisKeyConstant;
import com.aiops.context.BaseContext;
import com.aiops.converter.AnalysisJsonConverter;
import com.aiops.dto.AnalysisTaskCreateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCommentAnalysisResult;
import com.aiops.entity.BizProductCompareReport;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCommentAnalysisResultMapper;
import com.aiops.mapper.BizProductCompareReportMapper;
import com.aiops.service.AiCallLogService;
import com.aiops.service.CacheService;
import com.aiops.result.PageResult;
import com.aiops.service.AiRateLimitService;
import com.aiops.service.AnalysisService;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.AnalysisResultVO;
import com.aiops.vo.ProductCompareReportVO;
import com.aiops.vo.TaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private static final Pattern JAVA_MAP_LEFT_RIGHT_PATTERN =
            Pattern.compile("^\\{\\s*left=(.*?),\\s*right=(.*)\\s*}$", Pattern.DOTALL);
    private static final Pattern JAVA_MAP_RIGHT_LEFT_PATTERN =
            Pattern.compile("^\\{\\s*right=(.*?),\\s*left=(.*)\\s*}$", Pattern.DOTALL);

    private final BizAnalysisTaskMapper taskMapper;
    private final BizCommentAnalysisResultMapper resultMapper;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final CacheService cacheService;
    private final AnalysisJsonConverter analysisJsonConverter;
    private final PythonAiClient pythonAiClient;
    private final BizProductCompareReportMapper compareReportMapper;
    private final ObjectMapper objectMapper;
    private final AiRateLimitService aiRateLimitService;
    private final PromptTemplateService promptTemplateService;
    private final AiCallLogService aiCallLogService;

    @Override
    public TaskVO createAnalysisTask(AnalysisTaskCreateDTO createDTO) {
        validateCreateDTO(createDTO);
        BizAnalysisTask task = new BizAnalysisTask();
        task.setTargetType(createDTO.getTargetType());
        task.setTargetId(createDTO.getTargetId());
        task.setTaskType(createDTO.getAnalysisType());
        task.setTaskStatus("processing");
        task.setProgress(0);
        task.setStartTime(LocalDateTime.now());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        cacheTask(task.getId(), "processing", 0);

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", task.getId());
        request.put("targetType", createDTO.getTargetType());
        request.put("targetId", createDTO.getTargetId());
        try {
            Map<String, Object> response = pythonAnalysisClient.analyzeComments(request);
            validateAnalysisResponse(response);
            task.setTaskStatus("success");
            task.setProgress(100);
        } catch (Exception exception) {
            task.setTaskStatus("failed");
            task.setProgress(100);
            task.setErrorMessage(exception.getMessage());
        }
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        cacheTask(task.getId(), task.getTaskStatus(), task.getProgress());
        String status = cacheService.get(String.format(RedisKeyConstant.TASK_STATUS, task.getId()), String.class)
                .orElse(task.getTaskStatus());
        Integer progress = cacheService.get(String.format(RedisKeyConstant.TASK_PROGRESS, task.getId()), Integer.class)
                .orElse(task.getProgress());
        return new TaskVO(task.getId(), status, null, progress, null, null, null, task.getErrorMessage());
    }

    private void validateCreateDTO(AnalysisTaskCreateDTO createDTO) {
        if (createDTO == null) {
            throw new BusinessException(400, "分析任务参数不能为空");
        }
        if (createDTO.getTargetType() == null || createDTO.getTargetType().isBlank()) {
            throw new BusinessException(400, "目标类型不能为空");
        }
        if (createDTO.getTargetId() == null || createDTO.getTargetId().isBlank()) {
            throw new BusinessException(400, "目标 ID 不能为空");
        }
        createDTO.setTargetType(createDTO.getTargetType().trim());
        createDTO.setTargetId(createDTO.getTargetId().trim());
        createDTO.setAnalysisType(createDTO.getAnalysisType() == null || createDTO.getAnalysisType().isBlank()
                ? "comment_analysis"
                : createDTO.getAnalysisType().trim());
    }

    private void validateAnalysisResponse(Map<String, Object> response) {
        if (response == null || Boolean.FALSE.equals(response.get("success"))) {
            throw new BusinessException(503, stringValue(response, "message", "Python 评论分析服务返回失败"));
        }
        Integer totalCount = intValue(response, "totalCount");
        if (totalCount != null && totalCount == 0) {
            throw new BusinessException(404, "未找到可分析的评论，请确认商品 ID 是否完整");
        }
    }

    @Override
    public TaskVO getTask(Long taskId) {
        BizAnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "分析任务不存在");
        }
        String status = cacheService.get(String.format(RedisKeyConstant.TASK_STATUS, taskId), String.class)
                .orElse(task.getTaskStatus());
        Integer progress = cacheService.get(String.format(RedisKeyConstant.TASK_PROGRESS, taskId), Integer.class)
                .orElse(task.getProgress());
        return new TaskVO(task.getId(), status, null, progress, null, null, null, task.getErrorMessage());
    }

    @Override
    public AnalysisResultVO getProductAnalysis(String productId) {
        return getAnalysis("product", productId);
    }

    @Override
    public AnalysisResultVO getSellerAnalysis(String sellerId) {
        return getAnalysis("seller", sellerId);
    }

    private AnalysisResultVO getAnalysis(String targetType, String targetId) {
        String cacheKey = "product".equals(targetType)
                ? String.format(RedisKeyConstant.ANALYSIS_PRODUCT, targetId)
                : String.format(RedisKeyConstant.ANALYSIS_SELLER, targetId);
        Optional<AnalysisResultVO> cached = cacheService.get(cacheKey, AnalysisResultVO.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        BizCommentAnalysisResult result = resultMapper.selectOne(new LambdaQueryWrapper<BizCommentAnalysisResult>()
                .eq(BizCommentAnalysisResult::getTargetType, targetType)
                .eq(BizCommentAnalysisResult::getTargetId, targetId)
                .orderByDesc(BizCommentAnalysisResult::getCreateTime)
                .last("limit 1"));
        if (result == null) {
            throw new BusinessException(404, "分析结果不存在，请先发起评论分析任务");
        }
        AnalysisResultVO vo = toAnalysisResultVO(result);
        cacheService.set(cacheKey, vo, Duration.ofHours(6));
        return vo;
    }

    @Override
    public ProductCompareReportVO compareProducts(ProductCompareDTO compareDTO) {
        validateCompareDTO(compareDTO);
        String cacheKey = String.format(RedisKeyConstant.AI_PRODUCT_COMPARE,
                compareDTO.getLeftProductId(), compareDTO.getRightProductId());
        if (!Boolean.TRUE.equals(compareDTO.getForceRefresh())) {
            Optional<ProductCompareReportVO> cached = cacheService.get(cacheKey, ProductCompareReportVO.class);
            if (cached.isPresent()) {
                return cleanProductCompareReportVO(cached.get());
            }
        }
        checkAiRateLimit();

        BizCommentAnalysisResult leftResult = latestProductAnalysis(compareDTO.getLeftProductId());
        BizCommentAnalysisResult rightResult = latestProductAnalysis(compareDTO.getRightProductId());
        AnalysisResultVO leftVO = toAnalysisResultVO(leftResult);
        AnalysisResultVO rightVO = toAnalysisResultVO(rightResult);
        String metricSnapshot = toJson(Map.of("leftProduct", leftVO, "rightProduct", rightVO));

        Map<String, Object> request = new HashMap<>();
        request.put("leftProductId", compareDTO.getLeftProductId());
        request.put("rightProductId", compareDTO.getRightProductId());
        request.put("leftAnalysis", leftVO);
        request.put("rightAnalysis", rightVO);
        request.put("language", compareDTO.getLanguage());
        SysPromptTemplate template = attachPromptTemplate(request, "product_compare",
                compareDTO.getLanguage(), new HashMap<>(request));
        Map<String, Object> response = callPythonAi("product_compare", "product_pair",
                compareDTO.getLeftProductId() + ":" + compareDTO.getRightProductId(), templateId(template),
                () -> pythonAiClient.generateProductCompare(request));
        Map<String, Object> data = nestedData(response);

        BizProductCompareReport report = new BizProductCompareReport();
        report.setLeftProductId(compareDTO.getLeftProductId());
        report.setRightProductId(compareDTO.getRightProductId());
        report.setMetricSnapshot(metricSnapshot);
        report.setCompareSummary(stringValue(data, "compareSummary"));
        report.setAdvantageAnalysis(stringValue(data, "advantageAnalysis"));
        report.setRiskAnalysis(stringValue(data, "riskAnalysis"));
        report.setOperationSuggestions(stringValue(data, "operationSuggestions"));
        report.setModelName(stringValue(data, "modelName"));
        report.setCreateTime(LocalDateTime.now());
        compareReportMapper.insert(report);

        ProductCompareReportVO vo = toProductCompareReportVO(report);
        cacheService.set(cacheKey, vo, Duration.ofHours(12));
        return vo;
    }

    @Override
    public PageResult<ProductCompareReportVO> pageProductCompareReports(String leftProductId, String rightProductId,
                                                                        Integer pageNum, Integer pageSize) {
        Page<BizProductCompareReport> page = compareReportMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BizProductCompareReport>()
                        .eq(leftProductId != null, BizProductCompareReport::getLeftProductId, leftProductId)
                        .eq(rightProductId != null, BizProductCompareReport::getRightProductId, rightProductId)
                        .orderByDesc(BizProductCompareReport::getCreateTime));
        List<ProductCompareReportVO> records = page.getRecords().stream()
                .map(this::toProductCompareReportVO)
                .toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public ProductCompareReportVO getProductCompareReport(Long reportId) {
        BizProductCompareReport report = compareReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "商品对比报告不存在");
        }
        return toProductCompareReportVO(report);
    }

    private TaskVO toTaskVO(BizAnalysisTask task) {
        return new TaskVO(task.getId(), task.getTaskStatus(), null, task.getProgress(),
                null, null, null, task.getErrorMessage());
    }

    private AnalysisResultVO toAnalysisResultVO(BizCommentAnalysisResult result) {
        return new AnalysisResultVO(result.getTargetType(), result.getTargetId(),
                result.getTotalCount(), result.getPositiveCount(), result.getNeutralCount(),
                result.getNegativeCount(), result.getPositiveRate(), result.getNegativeRate(),
                analysisJsonConverter.parseKeywords(result.getTopKeywords()),
                analysisJsonConverter.parseKeywords(result.getNegativeKeywords()),
                analysisJsonConverter.parseDistributions(result.getScoreDistribution()),
                analysisJsonConverter.parseDistributions(result.getProblemDistribution()),
                analysisJsonConverter.parseDistributions(result.getCustomTagDistribution()),
                analysisJsonConverter.parseTrends(result.getTrendDistribution()),
                result.getSummary(), result.getCreateTime());
    }

    private BizCommentAnalysisResult latestProductAnalysis(String productId) {
        BizCommentAnalysisResult result = resultMapper.selectOne(new LambdaQueryWrapper<BizCommentAnalysisResult>()
                .eq(BizCommentAnalysisResult::getTargetType, "product")
                .eq(BizCommentAnalysisResult::getTargetId, productId)
                .orderByDesc(BizCommentAnalysisResult::getCreateTime)
                .last("limit 1"));
        if (result == null) {
            throw new BusinessException(404, "商品 " + productId + " 暂无分析结果，请先发起评论分析任务");
        }
        return result;
    }

    private ProductCompareReportVO toProductCompareReportVO(BizProductCompareReport report) {
        return new ProductCompareReportVO(report.getId(), report.getLeftProductId(), report.getRightProductId(),
                report.getMetricSnapshot(), cleanCompareSection(report.getCompareSummary()),
                cleanCompareSection(report.getAdvantageAnalysis()),
                cleanCompareSection(report.getRiskAnalysis()), cleanCompareSection(report.getOperationSuggestions()),
                report.getModelName(),
                report.getCreateTime());
    }

    private ProductCompareReportVO cleanProductCompareReportVO(ProductCompareReportVO report) {
        return new ProductCompareReportVO(report.getReportId(), report.getLeftProductId(), report.getRightProductId(),
                report.getMetricSnapshot(), cleanCompareSection(report.getCompareSummary()),
                cleanCompareSection(report.getAdvantageAnalysis()),
                cleanCompareSection(report.getRiskAnalysis()), cleanCompareSection(report.getOperationSuggestions()),
                report.getModelName(), report.getCreateTime());
    }

    private void validateCompareDTO(ProductCompareDTO compareDTO) {
        if (compareDTO == null) {
            throw new BusinessException(400, "商品对比参数不能为空");
        }
        if (compareDTO.getLeftProductId() == null || compareDTO.getLeftProductId().isBlank()) {
            throw new BusinessException(400, "左侧商品 ID 不能为空");
        }
        if (compareDTO.getRightProductId() == null || compareDTO.getRightProductId().isBlank()) {
            throw new BusinessException(400, "右侧商品 ID 不能为空");
        }
        if (compareDTO.getLeftProductId().equals(compareDTO.getRightProductId())) {
            throw new BusinessException(400, "请选择两个不同商品进行对比");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "对比指标序列化失败");
        }
    }

    private Map<String, Object> callPythonAi(String businessType, String targetType, String targetId,
                                             Long promptTemplateId, PythonAiCall call) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> response = call.execute();
            if (response == null || Boolean.FALSE.equals(response.get("success"))) {
                throw new BusinessException(503, "Python AI 服务返回失败");
            }
            recordAiCall(businessType, targetType, targetId, promptTemplateId,
                    modelName(response), tokenUsage(response), latencyMs(startedAt), null);
            return response;
        } catch (BusinessException exception) {
            recordAiCallFailure(businessType, targetType, targetId, promptTemplateId, latencyMs(startedAt), exception);
            throw exception;
        } catch (Exception exception) {
            recordAiCallFailure(businessType, targetType, targetId, promptTemplateId, latencyMs(startedAt), exception);
            throw new BusinessException(503, "Python AI 服务不可用：" + exception.getMessage());
        }
    }

    private SysPromptTemplate attachPromptTemplate(Map<String, Object> request, String businessType,
                                                   String language, Map<String, Object> variables) {
        Optional<SysPromptTemplate> template = Optional.ofNullable(promptTemplateService.findDefaultTemplate(businessType, language))
                .orElse(Optional.empty());
        template.ifPresent(value -> {
            request.put("promptTemplateId", value.getId());
            request.put("promptTemplate", value.getTemplateContent());
            request.put("promptVariables", variables);
        });
        return template.orElse(null);
    }

    private void recordAiCall(String businessType, String targetType, String targetId, Long promptTemplateId,
                              String modelName, Integer tokenUsage, Long latencyMs, String errorMessage) {
        try {
            aiCallLogService.record(BaseContext.getCurrentId(), businessType, targetType, targetId, promptTemplateId,
                    modelName, errorMessage == null ? "success" : "failed", tokenUsage, latencyMs, errorMessage);
        } catch (Exception ignored) {
            // AI logging must not break product comparison generation.
        }
    }

    private void recordAiCallFailure(String businessType, String targetType, String targetId, Long promptTemplateId,
                                     Long latencyMs, Exception exception) {
        recordAiCall(businessType, targetType, targetId, promptTemplateId, null, 0, latencyMs, exception.getMessage());
    }

    private Long latencyMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private Long templateId(SysPromptTemplate template) {
        return template == null ? null : template.getId();
    }

    private String modelName(Map<String, Object> response) {
        String topLevel = stringValue(response, "modelName");
        if (topLevel != null) {
            return topLevel;
        }
        return stringValue(nestedData(response), "modelName");
    }

    private Integer tokenUsage(Map<String, Object> response) {
        Integer topLevel = intValue(response, "tokenUsage");
        if (topLevel != null) {
            return topLevel;
        }
        return intValue(nestedData(response), "tokenUsage");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedData(Map<String, Object> response) {
        Object data = response.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : response;
    }

    private String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return plainText(value);
    }

    private String stringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        String value = plainText(map.get(key));
        return value == null ? defaultValue : value;
    }

    private String plainText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> sectionMap) {
            return mapToPlainText(sectionMap);
        }
        if (value instanceof Collection<?> collection) {
            List<String> lines = new ArrayList<>();
            for (Object item : collection) {
                String text = plainText(item);
                if (text != null && !text.isBlank()) {
                    lines.add(text);
                }
            }
            return String.join(System.lineSeparator(), lines);
        }
        return cleanCompareSection(String.valueOf(value));
    }

    private String mapToPlainText(Map<?, ?> sectionMap) {
        List<String> lines = new ArrayList<>();
        appendCompareLine(lines, sectionMap, "left", "左侧");
        appendCompareLine(lines, sectionMap, "right", "右侧");
        for (Map.Entry<?, ?> entry : sectionMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if ("left".equals(key) || "right".equals(key)) {
                continue;
            }
            String text = plainText(entry.getValue());
            if (text != null && !text.isBlank()) {
                lines.add(key + "：" + text);
            }
        }
        return String.join(System.lineSeparator(), lines);
    }

    private void appendCompareLine(List<String> lines, Map<?, ?> sectionMap, String key, String label) {
        if (!sectionMap.containsKey(key)) {
            return;
        }
        String text = plainText(sectionMap.get(key));
        if (text != null && !text.isBlank()) {
            lines.add(label + "：" + text);
        }
    }

    private String cleanCompareSection(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        Matcher leftRightMatcher = JAVA_MAP_LEFT_RIGHT_PATTERN.matcher(trimmed);
        if (leftRightMatcher.matches()) {
            return "左侧：" + leftRightMatcher.group(1).trim()
                    + System.lineSeparator()
                    + "右侧：" + leftRightMatcher.group(2).trim();
        }
        Matcher rightLeftMatcher = JAVA_MAP_RIGHT_LEFT_PATTERN.matcher(trimmed);
        if (rightLeftMatcher.matches()) {
            return "左侧：" + rightLeftMatcher.group(2).trim()
                    + System.lineSeparator()
                    + "右侧：" + rightLeftMatcher.group(1).trim();
        }
        return trimmed;
    }

    private Integer intValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void checkAiRateLimit() {
        Long userId = BaseContext.getCurrentId();
        if (!aiRateLimitService.tryConsume("product-compare", userId)) {
            throw new BusinessException(429, "AI 调用过于频繁，请稍后再试");
        }
    }

    private void cacheTask(Long taskId, String status, Integer progress) {
        cacheService.set(String.format(RedisKeyConstant.TASK_STATUS, taskId), status, Duration.ofHours(2));
        cacheService.set(String.format(RedisKeyConstant.TASK_PROGRESS, taskId), progress, Duration.ofHours(2));
    }

    private interface PythonAiCall {
        Map<String, Object> execute();
    }
}
