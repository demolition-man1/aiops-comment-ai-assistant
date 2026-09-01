package com.aiops.service.impl;

import com.aiops.context.BaseContext;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobEventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobEventServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private BizAiExecutionDetailMapper executionDetailMapper;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void subscriptionRejectsAJobOwnedByAnotherUser() {
        BaseContext.setCurrentId(9L);
        BizAnalysisTask task = task(42L, "processing", 20);
        when(taskMapper.selectById(7L)).thenReturn(task);

        AiJobEventService service = new AiJobEventServiceImpl(taskMapper, executionDetailMapper);

        assertThatThrownBy(() -> service.subscribe(7L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI 任务不存在");
    }

    @Test
    void validStageEventPersistsNewStageProgressAndVersion() {
        BizAnalysisTask task = task(9L, "processing", 20);
        BizAiExecutionDetail detail = detail("preparing", 3);
        when(taskMapper.selectById(7L)).thenReturn(task);
        when(executionDetailMapper.selectById(7L)).thenReturn(detail);
        AiJobEventService service = new AiJobEventServiceImpl(taskMapper, executionDetailMapper);

        service.publishStage(7L, "generating", 55);

        ArgumentCaptor<BizAnalysisTask> taskCaptor = ArgumentCaptor.forClass(BizAnalysisTask.class);
        ArgumentCaptor<BizAiExecutionDetail> detailCaptor = ArgumentCaptor.forClass(BizAiExecutionDetail.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        verify(executionDetailMapper).updateById(detailCaptor.capture());
        assertThat(taskCaptor.getValue().getProgress()).isEqualTo(55);
        assertThat(detailCaptor.getValue().getJobStage()).isEqualTo("generating");
        assertThat(detailCaptor.getValue().getVersion()).isEqualTo(4);
    }

    @Test
    void terminalJobIgnoresLateStageEvent() {
        when(taskMapper.selectById(7L)).thenReturn(task(9L, "success", 100));
        when(executionDetailMapper.selectById(7L)).thenReturn(detail("persisting", 3));
        AiJobEventService service = new AiJobEventServiceImpl(taskMapper, executionDetailMapper);

        service.publishStage(7L, "generating", 55);

        verify(taskMapper, never()).updateById(task(9L, "success", 100));
        verify(executionDetailMapper, never()).updateById(detail("persisting", 3));
    }

    private BizAnalysisTask task(Long userId, String status, Integer progress) {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(7L);
        task.setUserId(userId);
        task.setTaskType("operation_report");
        task.setTaskStatus(status);
        task.setProgress(progress);
        return task;
    }

    private BizAiExecutionDetail detail(String stage, Integer version) {
        BizAiExecutionDetail detail = new BizAiExecutionDetail();
        detail.setTaskId(7L);
        detail.setJobStage(stage);
        detail.setVersion(version);
        return detail;
    }
}
