package com.aiops.service.impl;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.aijob.AiJobLeaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobRecoveryServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;
    @Mock
    private BizAiExecutionDetailMapper detailMapper;
    @Mock
    private AiJobExecutionService executionService;
    @Mock
    private AiJobLeaseService leaseService;

    @Test
    void recoveryResubmitsPendingButOnlyFailsExpiredProcessingJobs() {
        BizAnalysisTask pending = task(11L, "pending");
        BizAnalysisTask live = task(12L, "processing");
        BizAnalysisTask expired = task(13L, "processing");
        when(taskMapper.selectList(any())).thenReturn(List.of(pending, live, expired));
        BizAiExecutionDetail pendingDetail = new BizAiExecutionDetail();
        BizAiExecutionDetail liveDetail = new BizAiExecutionDetail();
        BizAiExecutionDetail expiredDetail = new BizAiExecutionDetail();
        when(detailMapper.selectById(11L)).thenReturn(pendingDetail);
        when(detailMapper.selectById(12L)).thenReturn(liveDetail);
        when(detailMapper.selectById(13L)).thenReturn(expiredDetail);
        when(leaseService.isLive(any(BizAiExecutionDetail.class))).thenAnswer(invocation ->
                invocation.getArgument(0) == liveDetail);

        AiJobRecoveryServiceImpl service = new AiJobRecoveryServiceImpl(
                taskMapper, detailMapper, executionService, leaseService);

        int recovered = service.recoverJobs();

        assertThat(recovered).isEqualTo(2);
        verify(executionService).submit(11L);
        verify(executionService, never()).submit(12L);
        verify(executionService, never()).submit(13L);
        ArgumentCaptor<BizAnalysisTask> taskCaptor = ArgumentCaptor.forClass(BizAnalysisTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getId()).isEqualTo(13L);
        assertThat(taskCaptor.getValue().getTaskStatus()).isEqualTo("failed");
        assertThat(taskCaptor.getValue().getErrorMessage()).contains("worker_interrupted");
    }

    private BizAnalysisTask task(Long id, String status) {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(id);
        task.setTaskType("operation_report");
        task.setTaskStatus(status);
        task.setUpdateTime(LocalDateTime.now().minusMinutes(1));
        return task;
    }
}
