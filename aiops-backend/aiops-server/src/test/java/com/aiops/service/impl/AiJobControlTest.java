package com.aiops.service.impl;

import com.aiops.constant.RedisKeyConstant;
import com.aiops.context.BaseContext;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobEventService;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.CacheService;
import com.aiops.vo.AiJobCreatedVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class AiJobControlTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;
    @Mock
    private BizAiExecutionDetailMapper executionDetailMapper;
    @Mock
    private AiJobExecutionService executionService;
    @Mock
    private CacheService cacheService;
    @Mock
    private AiJobEventService eventService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void pendingJobCancelsBeforeItCanRun() {
        BaseContext.setCurrentId(9L);
        BizAnalysisTask task = task(41L, "pending");
        BizAiExecutionDetail detail = detail(41L, 1, null);
        when(taskMapper.selectById(41L)).thenReturn(task);
        when(executionDetailMapper.selectById(41L)).thenReturn(detail);

        service().cancelOwnedJob(41L);

        assertThat(task.getTaskStatus()).isEqualTo("cancelled");
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(detail.getCancelRequested()).isEqualTo(1);
        verify(cacheService).set(eq(String.format(RedisKeyConstant.AI_JOB_CANCEL, 41L)), eq("1"), any(Duration.class));
        verify(eventService).publishTerminal(41L);
        var order = inOrder(cacheService, executionDetailMapper);
        order.verify(cacheService).set(eq(String.format(RedisKeyConstant.AI_JOB_CANCEL, 41L)), eq("1"), any(Duration.class));
        order.verify(executionDetailMapper).updateById(detail);
    }

    @Test
    void processingJobRetainsItsStateButSignalsCancellationToProvider() {
        BaseContext.setCurrentId(9L);
        BizAnalysisTask task = task(41L, "processing");
        BizAiExecutionDetail detail = detail(41L, 1, null);
        when(taskMapper.selectById(41L)).thenReturn(task);
        when(executionDetailMapper.selectById(41L)).thenReturn(detail);

        service().cancelOwnedJob(41L);

        assertThat(task.getTaskStatus()).isEqualTo("processing");
        assertThat(detail.getCancelRequested()).isEqualTo(1);
        verify(executionDetailMapper).updateById(detail);
    }

    @Test
    void retryCreatesLinkedNewAttemptForTerminalJob() {
        BaseContext.setCurrentId(9L);
        BizAnalysisTask original = task(41L, "failed");
        original.setRequestParam("{\"productId\":\"p-1\"}");
        BizAiExecutionDetail detail = detail(41L, 2, null);
        when(taskMapper.selectById(41L)).thenReturn(original);
        when(executionDetailMapper.selectById(41L)).thenReturn(detail);
        when(taskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, BizAnalysisTask.class).setId(42L);
            return 1;
        });

        AiJobCreatedVO created = service().retryOwnedJob(41L);

        ArgumentCaptor<BizAiExecutionDetail> detailCaptor = ArgumentCaptor.forClass(BizAiExecutionDetail.class);
        verify(executionDetailMapper).insert(detailCaptor.capture());
        assertThat(created.jobId()).isEqualTo(42L);
        assertThat(detailCaptor.getValue().getParentTaskId()).isEqualTo(41L);
        assertThat(detailCaptor.getValue().getAttemptCount()).isEqualTo(3);
        assertThat(detailCaptor.getValue().getIdempotencyHash()).isNotEqualTo(detail.getIdempotencyHash());
        verify(executionService).submit(42L);
    }

    @Test
    void retryRejectsActiveJob() {
        BaseContext.setCurrentId(9L);
        when(taskMapper.selectById(41L)).thenReturn(task(41L, "processing"));
        when(executionDetailMapper.selectById(41L)).thenReturn(detail(41L, 1, null));

        assertThatThrownBy(() -> service().retryOwnedJob(41L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("执行中");
    }

    private AiJobServiceImpl service() {
        return new AiJobServiceImpl(taskMapper, executionDetailMapper, executionService, cacheService,
                eventService, new ObjectMapper());
    }

    private BizAnalysisTask task(Long id, String status) {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(id);
        task.setUserId(9L);
        task.setTargetType("product");
        task.setTargetId("p-1");
        task.setTaskType("operation_report");
        task.setTaskStatus(status);
        task.setProgress("pending".equals(status) ? 0 : 60);
        return task;
    }

    private BizAiExecutionDetail detail(Long taskId, Integer attempts, String idempotencyHash) {
        BizAiExecutionDetail detail = new BizAiExecutionDetail();
        detail.setTaskId(taskId);
        detail.setAttemptCount(attempts);
        detail.setIdempotencyHash(idempotencyHash == null ? "original-key" : idempotencyHash);
        detail.setRequestHash("request-hash");
        detail.setVersion(1);
        detail.setJobStage("generating");
        detail.setCancelRequested(0);
        return detail;
    }
}
