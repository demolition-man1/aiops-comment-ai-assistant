package com.aiops.service.impl;

import com.aiops.context.AiJobContext;
import com.aiops.constant.RedisKeyConstant;
import com.aiops.context.BaseContext;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.AiJobEventService;
import com.aiops.service.AiCallLogService;
import com.aiops.service.CacheService;
import com.aiops.service.aijob.AiJobCompletionService;
import com.aiops.service.aijob.AiJobExecutionResult;
import com.aiops.service.aijob.AiJobHandlerRegistry;
import com.aiops.service.aijob.AiJobConcurrencyGuard;
import com.aiops.service.aijob.AiJobLeaseService;
import com.aiops.properties.AiJobProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Service
public class AiJobExecutionServiceImpl implements AiJobExecutionService {

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;
    private final TaskExecutor aiJobExecutor;
    private final AiJobHandlerRegistry handlerRegistry;
    private final AiJobCompletionService completionService;
    private final AiJobEventService eventService;
    private final CacheService cacheService;
    private final AiCallLogService aiCallLogService;
    private final AiJobConcurrencyGuard concurrencyGuard;
    private final AiJobLeaseService leaseService;
    private final AiJobProperties properties;
    private final TaskScheduler heartbeatScheduler;
    private final ObjectProvider<AiJobExecutionService> selfProvider;

    @Autowired
    public AiJobExecutionServiceImpl(BizAnalysisTaskMapper taskMapper,
                                     BizAiExecutionDetailMapper executionDetailMapper,
                                     @Qualifier("aiJobExecutor") TaskExecutor aiJobExecutor,
                                     AiJobHandlerRegistry handlerRegistry,
                                     AiJobCompletionService completionService,
                                     AiJobEventService eventService,
                                     CacheService cacheService,
                                     AiCallLogService aiCallLogService,
                                     AiJobConcurrencyGuard concurrencyGuard,
                                     AiJobLeaseService leaseService,
                                     AiJobProperties properties,
                                     @Qualifier("aiJobHeartbeatScheduler") TaskScheduler heartbeatScheduler,
                                     ObjectProvider<AiJobExecutionService> selfProvider) {
        this.taskMapper = taskMapper;
        this.executionDetailMapper = executionDetailMapper;
        this.aiJobExecutor = aiJobExecutor;
        this.handlerRegistry = handlerRegistry;
        this.completionService = completionService;
        this.eventService = eventService;
        this.cacheService = cacheService;
        this.aiCallLogService = aiCallLogService;
        this.concurrencyGuard = concurrencyGuard;
        this.leaseService = leaseService;
        this.properties = properties;
        this.heartbeatScheduler = heartbeatScheduler;
        this.selfProvider = selfProvider;
    }

    AiJobExecutionServiceImpl(BizAnalysisTaskMapper taskMapper,
                              BizAiExecutionDetailMapper executionDetailMapper,
                              TaskExecutor aiJobExecutor) {
        this(taskMapper, executionDetailMapper, aiJobExecutor, null, null, null, null,
                null, null, null, null, null, null);
    }

    @Override
    public void submit(Long jobId) {
        aiJobExecutor.execute(() -> execute(jobId, "ai-job-" + UUID.randomUUID()));
    }

    @Override
    @Transactional
    public boolean claim(Long jobId, String leaseOwner) {
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        return detail != null && claim(detail, leaseOwner);
    }

    boolean claim(BizAiExecutionDetail detail, String leaseOwner) {
        if (detail.getTaskId() == null || detail.getVersion() == null || leaseOwner == null || leaseOwner.isBlank()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (taskMapper.markAiJobProcessing(detail.getTaskId(), now) != 1) {
            return false;
        }
        if (leaseService != null) {
            return leaseService.claim(detail, leaseOwner);
        }
        return executionDetailMapper.claimLease(detail.getTaskId(), detail.getVersion(), leaseOwner,
                now.plusSeconds(45), now) == 1;
    }

    private void execute(Long jobId, String leaseOwner) {
        BizAnalysisTask task = taskMapper.selectById(jobId);
        AiJobExecutionService proxiedService = selfProvider == null ? this : selfProvider.getObject();
        if (task == null || handlerRegistry == null || completionService == null) {
            return;
        }
        AiJobConcurrencyGuard.Permit permit = concurrencyGuard == null
                ? null
                : concurrencyGuard.tryAcquire(task.getUserId()).orElse(null);
        if (concurrencyGuard != null && permit == null) {
            return;
        }
        if (!proxiedService.claim(jobId, leaseOwner)) {
            releasePermit(permit);
            return;
        }
        recordQueueLatency(task, jobId);
        ScheduledFuture<?> heartbeat = startHeartbeat(jobId, leaseOwner);
        try {
            BaseContext.setCurrentId(task.getUserId());
            AiJobContext.set(task.getId(), task.getTaskType());
            AiJobExecutionResult result = completionService.complete(task.getId(),
                    () -> handlerRegistry.require(task.getTaskType()).execute(task));
            if (result != null) {
                completeCallLog(task.getId(), null);
                publishTerminal(task.getId());
            }
        } catch (Exception exception) {
            markFailed(task.getId(), exception);
            completeCallLog(task.getId(), errorCode(exception));
            publishTerminal(task.getId());
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            if (leaseService != null) {
                leaseService.release(jobId, leaseOwner);
            }
            releasePermit(permit);
            AiJobContext.remove();
            BaseContext.removeCurrentId();
        }
    }

    private void markFailed(Long taskId, Exception exception) {
        BizAnalysisTask task = taskMapper.selectById(taskId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(taskId);
        if (task == null || "cancelled".equals(task.getTaskStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if ((detail != null && Integer.valueOf(1).equals(detail.getCancelRequested())) || isCancellationSignalled(taskId)) {
            task.setTaskStatus("cancelled");
            task.setProgress(100);
            task.setEndTime(now);
            task.setUpdateTime(now);
            taskMapper.updateById(task);
            return;
        }
        task.setTaskStatus("failed");
        task.setProgress(100);
        task.setErrorMessage(publicErrorMessage(exception));
        task.setEndTime(now);
        task.setUpdateTime(now);
        taskMapper.updateById(task);
        if (detail != null) {
            detail.setErrorCode(errorCode(exception));
            detail.setUpdateTime(now);
            executionDetailMapper.updateById(detail);
        }
    }

    private void publishTerminal(Long taskId) {
        if (eventService != null) {
            eventService.publishTerminal(taskId);
        }
    }

    private boolean isCancellationSignalled(Long taskId) {
        return cacheService != null && cacheService.get(String.format(RedisKeyConstant.AI_JOB_CANCEL, taskId), String.class)
                .isPresent();
    }

    private String publicErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "AI 任务执行失败" : message;
    }

    private void recordQueueLatency(BizAnalysisTask task, Long jobId) {
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (detail == null || task.getCreateTime() == null) {
            return;
        }
        detail.setQueueLatencyMs(Math.max(0, Duration.between(task.getCreateTime(), LocalDateTime.now()).toMillis()));
        detail.setUpdateTime(LocalDateTime.now());
        executionDetailMapper.updateById(detail);
    }

    private ScheduledFuture<?> startHeartbeat(Long jobId, String leaseOwner) {
        if (leaseService == null || properties == null || heartbeatScheduler == null) {
            return null;
        }
        return heartbeatScheduler.scheduleAtFixedRate(
                () -> leaseService.renew(jobId, leaseOwner),
                Duration.ofSeconds(properties.getHeartbeatSeconds()));
    }

    private void releasePermit(AiJobConcurrencyGuard.Permit permit) {
        if (concurrencyGuard != null && permit != null) {
            concurrencyGuard.release(permit);
        }
    }

    private String errorCode(Exception exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (isCancellationSignalled(AiJobContext.getJobId())) return "cancelled";
        if (message.contains("timeout")) return "provider_timeout";
        if (message.contains("401") || message.contains("403") || message.contains("auth")) return "authentication";
        if (message.contains("429") || message.contains("rate limit")) return "rate_limited";
        if (message.contains("reject")) return "provider_rejected";
        if (message.contains("validation") || message.contains("invalid output")) return "invalid_output";
        if (message.contains("connection") || message.contains("temporar")) return "provider_temporary";
        return "internal";
    }

    private void completeCallLog(Long jobId, String fallbackErrorCode) {
        if (aiCallLogService == null) {
            return;
        }
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (detail == null) {
            return;
        }
        aiCallLogService.completeJob(jobId, detail.getQueueLatencyMs(), detail.getTotalLatencyMs(),
                detail.getErrorCode() == null ? fallbackErrorCode : detail.getErrorCode());
    }
}
