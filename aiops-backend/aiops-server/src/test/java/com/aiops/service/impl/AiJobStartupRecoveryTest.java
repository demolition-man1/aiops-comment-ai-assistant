package com.aiops.service.impl;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.task.AiJobStartupRecovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobStartupRecoveryTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private BizAiExecutionDetailMapper executionDetailMapper;

    @Mock
    private AiJobExecutionService executionService;

    @Test
    void startupResubmitsOnlyPendingTasksThatHaveAiExecutionDetails() {
        BizAnalysisTask pending = task(61L, "pending");
        BizAnalysisTask processing = task(62L, "processing");
        when(taskMapper.selectList(any())).thenReturn(List.of(pending, processing));
        when(executionDetailMapper.selectById(61L)).thenReturn(new BizAiExecutionDetail());

        AiJobRecoveryServiceImpl recoveryService = new AiJobRecoveryServiceImpl(
                taskMapper, executionDetailMapper, executionService);
        int submitted = recoveryService.resubmitPendingJobs();

        assertThat(submitted).isEqualTo(1);
        verify(executionService).submit(61L);
        verify(executionService, never()).submit(62L);
        new AiJobStartupRecovery(recoveryService).onApplicationEvent(null);
        verify(executionService, times(2)).submit(61L);
    }

    private BizAnalysisTask task(Long id, String status) {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(id);
        task.setTaskType("operation_report");
        task.setTaskStatus(status);
        return task;
    }
}
