package com.aiops.service.impl;

import com.aiops.context.BaseContext;
import com.aiops.constant.RedisKeyConstant;
import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.result.PageResult;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.AiJobEventService;
import com.aiops.service.AiJobService;
import com.aiops.service.CacheService;
import com.aiops.vo.AiJobCreatedVO;
import com.aiops.vo.AiJobVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJobServiceImpl implements AiJobService {

    private static final List<String> SUPPORTED_JOB_TYPES = List.of(
            "operation_report", "product_compare", "negative_reply", "content");

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;
    private final AiJobExecutionService executionService;
    private final CacheService cacheService;
    private final AiJobEventService eventService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiJobCreatedVO createReportJob(AiReportGenerateDTO dto, String idempotencyKey) {
        if (dto == null) {
            throw new BusinessException(400, "运营报告任务参数不能为空");
        }
        String productId = trimToNull(dto.getProductId());
        String sellerId = trimToNull(dto.getSellerId());
        if ((productId == null) == (sellerId == null)) {
            throw new BusinessException(400, "运营报告任务必须且只能指定一个目标");
        }
        dto.setProductId(productId);
        dto.setSellerId(sellerId);
        dto.setLanguage(defaultLanguage(dto.getLanguage()));
        String targetType = productId == null ? "seller" : "product";
        String targetId = productId == null ? sellerId : productId;
        return createJob("operation_report", targetType, targetId, dto, idempotencyKey);
    }

    @Override
    @Transactional
    public AiJobCreatedVO createProductCompareJob(ProductCompareDTO dto, String idempotencyKey) {
        if (dto == null) {
            throw new BusinessException(400, "商品对比任务参数不能为空");
        }
        String leftProductId = trimToNull(dto.getLeftProductId());
        String rightProductId = trimToNull(dto.getRightProductId());
        if (leftProductId == null || rightProductId == null) {
            throw new BusinessException(400, "两个商品 ID 都不能为空");
        }
        if (leftProductId.equals(rightProductId)) {
            throw new BusinessException(400, "请选择两个不同商品进行对比");
        }
        dto.setLeftProductId(leftProductId);
        dto.setRightProductId(rightProductId);
        dto.setLanguage(defaultLanguage(dto.getLanguage()));
        return createJob("product_compare", "product_pair", leftProductId + ":" + rightProductId, dto, idempotencyKey);
    }

    @Override
    @Transactional
    public AiJobCreatedVO createNegativeReplyJob(NegativeReplyGenerateDTO dto, String idempotencyKey) {
        if (dto == null || dto.getCommentId() == null || dto.getCommentId() <= 0) {
            throw new BusinessException(400, "评论 ID 不能为空");
        }
        dto.setToneType(trimToNull(dto.getToneType()) == null ? "sincere" : dto.getToneType().trim());
        dto.setLanguage(defaultLanguage(dto.getLanguage()));
        return createJob("negative_reply", "comment", String.valueOf(dto.getCommentId()), dto, idempotencyKey);
    }

    @Override
    @Transactional
    public AiJobCreatedVO createContentJob(AiContentGenerateDTO dto, String idempotencyKey) {
        if (dto == null || trimToNull(dto.getTargetType()) == null || trimToNull(dto.getTargetId()) == null
                || trimToNull(dto.getContentType()) == null) {
            throw new BusinessException(400, "AI 文案任务参数不能为空");
        }
        dto.setTargetType(dto.getTargetType().trim());
        dto.setTargetId(dto.getTargetId().trim());
        dto.setContentType(dto.getContentType().trim());
        dto.setStyleType(trimToNull(dto.getStyleType()) == null ? "simple" : dto.getStyleType().trim());
        dto.setLanguage(defaultLanguage(dto.getLanguage()));
        return createJob("content", dto.getTargetType(), dto.getTargetId(), dto, idempotencyKey);
    }

    @Override
    @Transactional
    public AiJobVO cancelOwnedJob(Long jobId) {
        BizAnalysisTask task = requireOwnedJob(jobId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (detail == null) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        if (isTerminal(task.getTaskStatus())) {
            throw new BusinessException(409, "已结束的 AI 任务不能取消");
        }
        LocalDateTime now = LocalDateTime.now();
        cacheService.set(String.format(RedisKeyConstant.AI_JOB_CANCEL, jobId), "1", Duration.ofMinutes(30));
        detail.setCancelRequested(1);
        detail.setVersion(defaultVersion(detail.getVersion()) + 1);
        detail.setUpdateTime(now);
        executionDetailMapper.updateById(detail);
        if ("pending".equals(task.getTaskStatus())) {
            task.setTaskStatus("cancelled");
            task.setProgress(100);
            task.setEndTime(now);
            task.setUpdateTime(now);
            taskMapper.updateById(task);
            publishTerminalAfterCommit(jobId);
        }
        return toJobVO(task, detail);
    }

    @Override
    @Transactional
    public AiJobCreatedVO retryOwnedJob(Long jobId) {
        BizAnalysisTask sourceTask = requireOwnedJob(jobId);
        BizAiExecutionDetail sourceDetail = executionDetailMapper.selectById(jobId);
        if (sourceDetail == null) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        if (!isRetryable(sourceTask.getTaskStatus())) {
            throw new BusinessException(409, "AI 任务执行中，暂不能重试");
        }
        LocalDateTime now = LocalDateTime.now();
        BizAnalysisTask retryTask = new BizAnalysisTask();
        retryTask.setUserId(sourceTask.getUserId());
        retryTask.setTargetType(sourceTask.getTargetType());
        retryTask.setTargetId(sourceTask.getTargetId());
        retryTask.setTaskType(sourceTask.getTaskType());
        retryTask.setTaskStatus("pending");
        retryTask.setProgress(0);
        retryTask.setRequestParam(sourceTask.getRequestParam());
        retryTask.setCreateTime(now);
        retryTask.setUpdateTime(now);
        taskMapper.insert(retryTask);
        BizAiExecutionDetail retryDetail = new BizAiExecutionDetail();
        retryDetail.setTaskId(retryTask.getId());
        retryDetail.setBusinessKey(sourceDetail.getBusinessKey());
        retryDetail.setIdempotencyHash(sha256("retry|" + sourceTask.getUserId() + "|" + jobId + "|" + UUID.randomUUID()));
        retryDetail.setRequestHash(sourceDetail.getRequestHash());
        retryDetail.setJobStage("preparing");
        retryDetail.setAttemptCount(defaultAttemptCount(sourceDetail.getAttemptCount()) + 1);
        retryDetail.setParentTaskId(sourceTask.getId());
        retryDetail.setCancelRequested(0);
        retryDetail.setVersion(0);
        retryDetail.setCreateTime(now);
        retryDetail.setUpdateTime(now);
        executionDetailMapper.insert(retryDetail);
        submitAfterCommit(retryTask.getId());
        return new AiJobCreatedVO(retryTask.getId(), "pending", false);
    }

    @Override
    public AiJobVO getOwnedJob(Long jobId) {
        BizAnalysisTask task = requireOwnedJob(jobId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (detail == null) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        return toJobVO(task, detail);
    }

    @Override
    public PageResult<AiJobVO> pageOwnedJobs(Integer pageNum, Integer pageSize, String jobType, String taskStatus) {
        Long userId = requireUserId();
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String normalizedJobType = trimToNull(jobType);
        if (normalizedJobType != null && !SUPPORTED_JOB_TYPES.contains(normalizedJobType)) {
            throw new BusinessException(400, "不支持的 AI 任务类型");
        }
        Page<BizAnalysisTask> page = taskMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<BizAnalysisTask>()
                        .eq(BizAnalysisTask::getUserId, userId)
                        .in(BizAnalysisTask::getTaskType, SUPPORTED_JOB_TYPES)
                        .eq(normalizedJobType != null, BizAnalysisTask::getTaskType, normalizedJobType)
                        .eq(trimToNull(taskStatus) != null, BizAnalysisTask::getTaskStatus, trimToNull(taskStatus))
                        .orderByDesc(BizAnalysisTask::getCreateTime));
        List<AiJobVO> records = page.getRecords().stream()
                .map(task -> {
                    BizAiExecutionDetail detail = executionDetailMapper.selectById(task.getId());
                    return detail == null ? null : toJobVO(task, detail);
                })
                .filter(job -> job != null)
                .toList();
        return PageResult.of(records, page.getTotal(), currentPage, currentSize);
    }

    private AiJobCreatedVO createJob(String jobType, String targetType, String targetId, Object dto, String idempotencyKey) {
        Long userId = requireUserId();
        String normalizedKey = requireIdempotencyKey(idempotencyKey);
        String requestJson = canonicalJson(dto);
        String requestHash = sha256(requestJson);
        String idempotencyHash = sha256(userId + "|" + jobType + "|" + normalizedKey);
        BizAiExecutionDetail existing = executionDetailMapper.selectOne(new LambdaQueryWrapper<BizAiExecutionDetail>()
                .eq(BizAiExecutionDetail::getIdempotencyHash, idempotencyHash)
                .last("limit 1"));
        if (existing != null) {
            return reuseOrReject(existing, requestHash);
        }

        LocalDateTime now = LocalDateTime.now();
        BizAnalysisTask task = new BizAnalysisTask();
        task.setUserId(userId);
        task.setTargetType(targetType);
        task.setTargetId(targetId);
        task.setTaskType(jobType);
        task.setTaskStatus("pending");
        task.setProgress(0);
        task.setRequestParam(requestJson);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        try {
            taskMapper.insert(task);
            BizAiExecutionDetail detail = new BizAiExecutionDetail();
            detail.setTaskId(task.getId());
            detail.setBusinessKey(jobType + ":" + targetType + ":" + targetId);
            detail.setIdempotencyHash(idempotencyHash);
            detail.setRequestHash(requestHash);
            detail.setJobStage("preparing");
            detail.setAttemptCount(1);
            detail.setCancelRequested(0);
            detail.setVersion(0);
            detail.setCreateTime(now);
            detail.setUpdateTime(now);
            executionDetailMapper.insert(detail);
        } catch (DuplicateKeyException exception) {
            BizAiExecutionDetail concurrent = executionDetailMapper.selectOne(new LambdaQueryWrapper<BizAiExecutionDetail>()
                    .eq(BizAiExecutionDetail::getIdempotencyHash, idempotencyHash)
                    .last("limit 1"));
            if (concurrent != null) {
                return reuseOrReject(concurrent, requestHash);
            }
            throw exception;
        }
        submitAfterCommit(task.getId());
        return new AiJobCreatedVO(task.getId(), "pending", false);
    }

    private AiJobCreatedVO reuseOrReject(BizAiExecutionDetail detail, String requestHash) {
        if (!requestHash.equals(detail.getRequestHash())) {
            throw new BusinessException(409, "幂等键已用于不同的 AI 任务请求");
        }
        return new AiJobCreatedVO(detail.getTaskId(), taskStatus(detail.getTaskId()), true);
    }

    private String taskStatus(Long taskId) {
        BizAnalysisTask task = taskMapper.selectById(taskId);
        return task == null ? "pending" : task.getTaskStatus();
    }

    private void submitAfterCommit(Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executionService.submit(taskId);
                }
            });
            return;
        }
        executionService.submit(taskId);
    }

    private void publishTerminalAfterCommit(Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventService.publishTerminal(taskId);
                }
            });
            return;
        }
        eventService.publishTerminal(taskId);
    }

    private BizAnalysisTask requireOwnedJob(Long jobId) {
        if (jobId == null) {
            throw new BusinessException(400, "AI 任务 ID 不能为空");
        }
        BizAnalysisTask task = taskMapper.selectById(jobId);
        if (task == null || !SUPPORTED_JOB_TYPES.contains(task.getTaskType())
                || !requireUserId().equals(task.getUserId())) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        return task;
    }

    private AiJobVO toJobVO(BizAnalysisTask task, BizAiExecutionDetail detail) {
        AiJobVO vo = new AiJobVO();
        vo.setJobId(task.getId());
        vo.setJobType(task.getTaskType());
        vo.setTargetType(task.getTargetType());
        vo.setTargetId(task.getTargetId());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setJobStage(detail.getJobStage());
        vo.setProgress(task.getProgress());
        vo.setResultType(detail.getResultType());
        vo.setResultId(detail.getResultId());
        vo.setAttemptCount(detail.getAttemptCount());
        vo.setCancelRequested(detail.getCancelRequested() != null && detail.getCancelRequested() == 1);
        vo.setQueueLatencyMs(detail.getQueueLatencyMs());
        vo.setProviderLatencyMs(detail.getProviderLatencyMs());
        vo.setTotalLatencyMs(detail.getTotalLatencyMs());
        vo.setErrorCode(detail.getErrorCode());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return userId;
    }

    private String requireIdempotencyKey(String key) {
        String normalized = trimToNull(key);
        if (normalized == null) {
            throw new BusinessException(400, "Idempotency-Key 不能为空");
        }
        if (normalized.length() > 128) {
            throw new BusinessException(400, "Idempotency-Key 不能超过 128 个字符");
        }
        return normalized;
    }

    private String canonicalJson(Object dto) {
        try {
            return objectMapper.copy()
                    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                    .writeValueAsString(dto);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "AI 任务参数序列化失败");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private boolean isTerminal(String status) {
        return "success".equals(status) || "failed".equals(status) || "timed_out".equals(status)
                || "cancelled".equals(status);
    }

    private boolean isRetryable(String status) {
        return "failed".equals(status) || "timed_out".equals(status) || "cancelled".equals(status);
    }

    private int defaultVersion(Integer version) {
        return version == null ? 0 : version;
    }

    private int defaultAttemptCount(Integer attemptCount) {
        return attemptCount == null ? 1 : attemptCount;
    }

    private String defaultLanguage(String language) {
        String normalized = trimToNull(language);
        return normalized == null ? "zh-CN" : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
