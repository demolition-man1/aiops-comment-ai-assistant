package com.aiops.service.impl;

import com.aiops.dto.AiCallLogQueryDTO;
import com.aiops.context.AiJobContext;
import com.aiops.context.BaseContext;
import com.aiops.entity.BizAiCallLog;
import com.aiops.mapper.BizAiCallLogMapper;
import com.aiops.result.PageResult;
import com.aiops.service.AiCallLogService;
import com.aiops.vo.AiCallLogOverviewVO;
import com.aiops.vo.AiCallLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCallLogServiceImpl implements AiCallLogService {

    private static final BigDecimal COST_PER_1K_TOKENS = new BigDecimal("0.0020");

    private final BizAiCallLogMapper aiCallLogMapper;

    @Override
    public PageResult<AiCallLogVO> pageLogs(AiCallLogQueryDTO queryDTO) {
        AiCallLogQueryDTO query = queryDTO == null ? new AiCallLogQueryDTO() : queryDTO;
        Page<BizAiCallLog> page = aiCallLogMapper.selectPage(new Page<>(
                        normalizePageNum(query.getPageNum()), normalizePageSize(query.getPageSize())),
                buildWrapper(query).orderByDesc(BizAiCallLog::getCreateTime));
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public AiCallLogOverviewVO overview(AiCallLogQueryDTO queryDTO) {
        List<BizAiCallLog> logs = aiCallLogMapper.selectList(buildWrapper(queryDTO == null ? new AiCallLogQueryDTO() : queryDTO));
        long totalCalls = logs.size();
        long successCalls = logs.stream().filter(log -> "success".equals(log.getCallStatus())).count();
        long failedCalls = logs.stream().filter(log -> "failed".equals(log.getCallStatus())).count();
        long totalTokens = logs.stream().map(BizAiCallLog::getTokenUsage).filter(value -> value != null)
                .mapToLong(Integer::longValue).sum();
        BigDecimal totalCost = logs.stream().map(BizAiCallLog::getEstimatedCost).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long latencyCount = logs.stream().filter(log -> log.getLatencyMs() != null).count();
        long totalLatency = sum(logs, BizAiCallLog::getLatencyMs);
        long queueCount = logs.stream().filter(log -> log.getQueueLatencyMs() != null).count();
        long totalQueue = sum(logs, BizAiCallLog::getQueueLatencyMs);
        long totalDurationCount = logs.stream().filter(log -> log.getTotalLatencyMs() != null).count();
        long totalDuration = sum(logs, BizAiCallLog::getTotalLatencyMs);
        BigDecimal successRate = totalCalls == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(successCalls)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCalls), 2, RoundingMode.HALF_UP);
        return new AiCallLogOverviewVO(totalCalls, successCalls, failedCalls, successRate, totalTokens,
                totalCost.setScale(6, RoundingMode.HALF_UP),
                latencyCount == 0 ? 0 : Math.round((double) totalLatency / latencyCount),
                queueCount == 0 ? 0 : Math.round((double) totalQueue / queueCount),
                totalDurationCount == 0 ? 0 : Math.round((double) totalDuration / totalDurationCount));
    }

    @Override
    public void record(Long userId, String businessType, String targetType, String targetId,
                       Long promptTemplateId, String modelName, String callStatus,
                       Integer tokenUsage, Long latencyMs, String errorMessage) {
        BizAiCallLog log = new BizAiCallLog();
        log.setJobId(AiJobContext.getJobId());
        log.setUserId(userId);
        log.setBusinessType(blankToDefault(businessType, "unknown"));
        log.setTargetType(blankToNull(targetType));
        log.setTargetId(blankToNull(targetId));
        log.setPromptTemplateId(promptTemplateId);
        log.setModelName(blankToNull(modelName));
        log.setCallStatus("failed".equals(callStatus) ? "failed" : "success");
        log.setTokenUsage(tokenUsage == null || tokenUsage < 0 ? 0 : tokenUsage);
        log.setEstimatedCost(estimateCost(log.getTokenUsage()));
        log.setLatencyMs(latencyMs == null || latencyMs < 0 ? 0 : latencyMs);
        log.setErrorMessage(trimError(errorMessage));
        log.setErrorCode(errorCode(errorMessage));
        log.setCreateTime(LocalDateTime.now());
        aiCallLogMapper.insert(log);
    }

    @Override
    public void completeJob(Long jobId, Long queueLatencyMs, Long totalLatencyMs, String errorCode) {
        if (jobId == null) {
            return;
        }
        aiCallLogMapper.updateJobObservability(jobId, nonNegative(queueLatencyMs), nonNegative(totalLatencyMs),
                normalizeErrorCode(errorCode));
    }

    private LambdaQueryWrapper<BizAiCallLog> buildWrapper(AiCallLogQueryDTO query) {
        String businessType = blankToNull(query.getBusinessType());
        String callStatus = blankToNull(query.getCallStatus());
        String targetType = blankToNull(query.getTargetType());
        String targetId = blankToNull(query.getTargetId());
        Long userId = BaseContext.getCurrentId();
        return new LambdaQueryWrapper<BizAiCallLog>()
                .eq(userId != null, BizAiCallLog::getUserId, userId)
                .isNotNull(userId == null, BizAiCallLog::getUserId)
                .eq(businessType != null, BizAiCallLog::getBusinessType, businessType)
                .eq(callStatus != null, BizAiCallLog::getCallStatus, callStatus)
                .eq(targetType != null, BizAiCallLog::getTargetType, targetType)
                .eq(targetId != null, BizAiCallLog::getTargetId, targetId);
    }

    private BigDecimal estimateCost(Integer tokenUsage) {
        if (tokenUsage == null || tokenUsage <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(tokenUsage)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(COST_PER_1K_TOKENS)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private AiCallLogVO toVO(BizAiCallLog log) {
        return new AiCallLogVO(log.getId(), log.getJobId(), log.getUserId(), log.getBusinessType(), log.getTargetType(),
                log.getTargetId(), log.getPromptTemplateId(), log.getModelName(), log.getCallStatus(),
                log.getTokenUsage(), log.getEstimatedCost(), log.getLatencyMs(), log.getQueueLatencyMs(),
                log.getTotalLatencyMs(), log.getErrorCode(), log.getErrorMessage(),
                log.getCreateTime());
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private String trimError(String errorMessage) {
        String value = blankToNull(errorMessage);
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private long sum(List<BizAiCallLog> logs, java.util.function.Function<BizAiCallLog, Long> extractor) {
        return logs.stream().map(extractor).filter(value -> value != null).mapToLong(Long::longValue).sum();
    }

    private Long nonNegative(Long value) {
        return value == null ? null : Math.max(0, value);
    }

    private String errorCode(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) return null;
        String message = errorMessage.toLowerCase();
        if (message.contains("timeout")) return "provider_timeout";
        if (message.contains("401") || message.contains("403") || message.contains("auth")) return "authentication";
        if (message.contains("429") || message.contains("rate limit")) return "rate_limited";
        if (message.contains("reject")) return "provider_rejected";
        if (message.contains("validation") || message.contains("invalid output")) return "invalid_output";
        if (message.contains("connection") || message.contains("temporar")) return "provider_temporary";
        return "internal";
    }

    private String normalizeErrorCode(String value) {
        return value != null && List.of("configuration", "authentication", "rate_limited", "provider_timeout",
                "provider_temporary", "provider_rejected", "invalid_output", "worker_interrupted", "cancelled", "internal")
                .contains(value) ? value : "internal";
    }
}
