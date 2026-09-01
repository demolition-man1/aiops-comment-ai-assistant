package com.aiops.service.impl;

import com.aiops.context.AiJobContext;
import com.aiops.context.BaseContext;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.aijob.AiJobCompletionService;
import com.aiops.service.aijob.AiJobExecutionResult;
import com.aiops.service.aijob.AiJobHandlerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiJobExecutionServiceImpl implements AiJobExecutionService {

    private static final int LEASE_SECONDS = 45;

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;
    private final TaskExecutor aiJobExecutor;
    private final AiJobHandlerRegistry handlerRegistry;
    private final AiJobCompletionService completionService;
    private final ObjectProvider<AiJobExecutionService> selfProvider;

    @Autowired
    public AiJobExecutionServiceImpl(BizAnalysisTaskMapper taskMapper,
                                     BizAiExecutionDetailMapper executionDetailMapper,
                                     @Qualifier("aiJobExecutor") TaskExecutor aiJobExecutor,
                                     AiJobHandlerRegistry handlerRegistry,
                                     AiJobCompletionService completionService,
                                     ObjectProvider<AiJobExecutionService> selfProvider) {
        this.taskMapper = taskMapper;
        this.executionDetailMapper = executionDetailMapper;
        this.aiJobExecutor = aiJobExecutor;
        this.handlerRegistry = handlerRegistry;
        this.completionService = completionService;
        this.selfProvider = selfProvider;
    }

    AiJobExecutionServiceImpl(BizAnalysisTaskMapper taskMapper,
                              BizAiExecutionDetailMapper executionDetailMapper,
                              TaskExecutor aiJobExecutor) {
        this(taskMapper, executionDetailMapper, aiJobExecutor, null, null, null);
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
        return executionDetailMapper.claimLease(detail.getTaskId(), detail.getVersion(), leaseOwner,
                now.plusSeconds(LEASE_SECONDS), now) == 1;
    }

    private void execute(Long jobId, String leaseOwner) {
        BizAnalysisTask task = taskMapper.selectById(jobId);
        AiJobExecutionService proxiedService = selfProvider == null ? this : selfProvider.getObject();
        if (task == null || handlerRegistry == null || completionService == null || !proxiedService.claim(jobId, leaseOwner)) {
            return;
        }
        try {
            BaseContext.setCurrentId(task.getUserId());
            AiJobContext.set(task.getId(), task.getTaskType());
            AiJobExecutionResult result = completionService.complete(task.getId(),
                    () -> handlerRegistry.require(task.getTaskType()).execute(task));
            if (result == null) {
                return;
            }
        } catch (Exception exception) {
            markFailed(task.getId(), exception);
        } finally {
            AiJobContext.remove();
            BaseContext.removeCurrentId();
        }
    }

    private void markFailed(Long taskId, Exception exception) {
        BizAnalysisTask task = taskMapper.selectById(taskId);
        if (task == null || "cancelled".equals(task.getTaskStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        task.setTaskStatus("failed");
        task.setProgress(100);
        task.setErrorMessage(publicErrorMessage(exception));
        task.setEndTime(now);
        task.setUpdateTime(now);
        taskMapper.updateById(task);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(taskId);
        if (detail != null) {
            detail.setErrorCode("internal");
            detail.setUpdateTime(now);
            executionDetailMapper.updateById(detail);
        }
    }

    private String publicErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "AI 任务执行失败" : message;
    }
}
