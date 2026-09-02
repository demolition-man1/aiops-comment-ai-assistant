package com.aiops.service.impl;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.AiJobRecoveryService;
import com.aiops.service.aijob.AiJobLeaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiJobRecoveryServiceImpl implements AiJobRecoveryService {

    private static final List<String> AI_JOB_TYPES = List.of(
            "operation_report", "product_compare", "negative_reply", "content");

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;
    private final AiJobExecutionService executionService;
    private final AiJobLeaseService leaseService;

    AiJobRecoveryServiceImpl(BizAnalysisTaskMapper taskMapper,
                             BizAiExecutionDetailMapper executionDetailMapper,
                             AiJobExecutionService executionService) {
        this(taskMapper, executionDetailMapper, executionService, null);
    }

    @Override
    public int resubmitPendingJobs() {
        List<BizAnalysisTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<BizAnalysisTask>()
                .eq(BizAnalysisTask::getTaskStatus, "pending")
                .in(BizAnalysisTask::getTaskType, AI_JOB_TYPES));
        int submitted = 0;
        for (BizAnalysisTask task : tasks) {
            if (!"pending".equals(task.getTaskStatus())) {
                continue;
            }
            BizAiExecutionDetail detail = executionDetailMapper.selectById(task.getId());
            if (detail == null) {
                continue;
            }
            executionService.submit(task.getId());
            submitted++;
        }
        return submitted;
    }

    @Override
    public int recoverJobs() {
        List<BizAnalysisTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<BizAnalysisTask>()
                .in(BizAnalysisTask::getTaskStatus, List.of("pending", "processing"))
                .in(BizAnalysisTask::getTaskType, AI_JOB_TYPES));
        int recovered = 0;
        for (BizAnalysisTask task : tasks) {
            BizAiExecutionDetail detail = executionDetailMapper.selectById(task.getId());
            if (detail == null) {
                continue;
            }
            if ("pending".equals(task.getTaskStatus())) {
                executionService.submit(task.getId());
                recovered++;
                continue;
            }
            if ("processing".equals(task.getTaskStatus()) && (leaseService == null || !leaseService.isLive(detail))) {
                markInterrupted(task, detail);
                recovered++;
            }
        }
        return recovered;
    }

    private void markInterrupted(BizAnalysisTask task, BizAiExecutionDetail detail) {
        LocalDateTime now = LocalDateTime.now();
        task.setTaskStatus("failed");
        task.setProgress(100);
        task.setErrorMessage("worker_interrupted");
        task.setEndTime(now);
        task.setUpdateTime(now);
        detail.setErrorCode("worker_interrupted");
        detail.setUpdateTime(now);
        taskMapper.updateById(task);
        executionDetailMapper.updateById(detail);
    }
}
