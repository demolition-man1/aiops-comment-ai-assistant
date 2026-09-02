package com.aiops.service.aijob;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AiJobCompletionServiceImpl implements AiJobCompletionService {

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;

    @Override
    @Transactional
    public AiJobExecutionResult complete(Long taskId, Supplier<AiJobExecutionResult> persistAction) {
        BizAnalysisTask task = taskMapper.selectById(taskId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(taskId);
        if (task == null || detail == null) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        if (Integer.valueOf(1).equals(detail.getCancelRequested())) {
            markCancelled(task);
            return null;
        }
        AiJobExecutionResult result = persistAction.get();
        detail = executionDetailMapper.selectByIdForUpdate(taskId);
        if (detail == null) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        if (Integer.valueOf(1).equals(detail.getCancelRequested())) {
            markCancelled(task);
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        detail.setResultType(result.resultType());
        detail.setResultId(result.resultId());
        detail.setModelName(result.modelName());
        detail.setInputTokens(result.inputTokens());
        detail.setOutputTokens(result.outputTokens());
        detail.setTotalTokens(result.totalTokens());
        detail.setTokenUsageEstimated(Boolean.TRUE.equals(result.tokenUsageEstimated()) ? 1 : 0);
        detail.setProviderLatencyMs(result.providerLatencyMs());
        detail.setTotalLatencyMs(Math.max(0, java.time.Duration.between(task.getCreateTime(), now).toMillis()));
        detail.setJobStage("persisting");
        detail.setUpdateTime(now);
        executionDetailMapper.updateById(detail);
        task.setTaskStatus("success");
        task.setProgress(100);
        task.setEndTime(now);
        task.setUpdateTime(now);
        taskMapper.updateById(task);
        return result;
    }

    private void markCancelled(BizAnalysisTask task) {
        LocalDateTime now = LocalDateTime.now();
        task.setTaskStatus("cancelled");
        task.setProgress(100);
        task.setEndTime(now);
        task.setUpdateTime(now);
        taskMapper.updateById(task);
    }
}
