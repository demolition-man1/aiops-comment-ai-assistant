package com.aiops.service.aijob;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobCompletionServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private BizAiExecutionDetailMapper executionDetailMapper;

    @Test
    void cancelledTaskDoesNotRunItsPersistenceAction() {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(33L);
        task.setTaskStatus("processing");
        BizAiExecutionDetail detail = new BizAiExecutionDetail();
        detail.setTaskId(33L);
        detail.setCancelRequested(1);
        when(taskMapper.selectById(33L)).thenReturn(task);
        when(executionDetailMapper.selectById(33L)).thenReturn(detail);
        AtomicBoolean persisted = new AtomicBoolean(false);

        AiJobExecutionResult result = new AiJobCompletionServiceImpl(taskMapper, executionDetailMapper)
                .complete(33L, () -> {
                    persisted.set(true);
                    return new AiJobExecutionResult("operation_report", 73L, "model", null, null, null, null, 2L);
                });

        assertThat(result).isNull();
        assertThat(persisted).isFalse();
        ArgumentCaptor<BizAnalysisTask> taskCaptor = ArgumentCaptor.forClass(BizAnalysisTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskStatus()).isEqualTo("cancelled");
        verify(executionDetailMapper, never()).selectByIdForUpdate(33L);
        verify(executionDetailMapper, never()).updateById(any(BizAiExecutionDetail.class));
    }

    @Test
    void cancellationArrivingDuringProviderCallPreventsLatePersistence() {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(34L);
        task.setTaskStatus("processing");
        BizAiExecutionDetail beforeProvider = new BizAiExecutionDetail();
        beforeProvider.setTaskId(34L);
        beforeProvider.setCancelRequested(0);
        BizAiExecutionDetail afterProvider = new BizAiExecutionDetail();
        afterProvider.setTaskId(34L);
        afterProvider.setCancelRequested(1);
        when(taskMapper.selectById(34L)).thenReturn(task);
        when(executionDetailMapper.selectById(34L)).thenReturn(beforeProvider);
        when(executionDetailMapper.selectByIdForUpdate(34L)).thenReturn(afterProvider);
        AtomicBoolean persisted = new AtomicBoolean(false);

        AiJobExecutionResult result = new AiJobCompletionServiceImpl(taskMapper, executionDetailMapper)
                .complete(34L, () -> {
                    persisted.set(true);
                    return new AiJobExecutionResult("operation_report", 74L, "model", null, null, null, null, 2L);
                });

        assertThat(result).isNull();
        assertThat(persisted).isTrue();
        verify(executionDetailMapper, never()).updateById(any(BizAiExecutionDetail.class));
    }
}
