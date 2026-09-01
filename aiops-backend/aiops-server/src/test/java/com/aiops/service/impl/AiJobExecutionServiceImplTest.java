package com.aiops.service.impl;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobExecutionServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private BizAiExecutionDetailMapper executionDetailMapper;

    @Test
    void onlyOneWorkerCanClaimTheSamePendingJob() {
        BizAiExecutionDetail detail = new BizAiExecutionDetail();
        detail.setTaskId(81L);
        detail.setVersion(3);
        when(taskMapper.markAiJobProcessing(eq(81L), any(LocalDateTime.class))).thenReturn(1, 0);
        when(executionDetailMapper.claimLease(eq(81L), eq(3), eq("worker-a"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);

        AiJobExecutionServiceImpl service = new AiJobExecutionServiceImpl(taskMapper, executionDetailMapper, runnable -> { });

        assertThat(service.claim(detail, "worker-a")).isTrue();
        assertThat(service.claim(detail, "worker-b")).isFalse();
    }

    @Test
    void publicClaimMethodIsTransactional() throws Exception {
        assertThat(AiJobExecutionServiceImpl.class
                .getMethod("claim", Long.class, String.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }
}
