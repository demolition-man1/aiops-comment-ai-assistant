package com.aiops.service.impl;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.AiJobRecoveryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiJobRecoveryServiceImpl implements AiJobRecoveryService {

    private static final List<String> AI_JOB_TYPES = List.of("operation_report", "product_compare");

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;
    private final AiJobExecutionService executionService;

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
}
