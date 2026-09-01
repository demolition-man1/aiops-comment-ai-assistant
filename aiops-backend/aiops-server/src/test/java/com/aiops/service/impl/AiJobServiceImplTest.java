package com.aiops.service.impl;

import com.aiops.context.BaseContext;
import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobExecutionService;
import com.aiops.service.AiJobEventService;
import com.aiops.service.CacheService;
import com.aiops.vo.AiJobCreatedVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobServiceImplTest {

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
    void identicalOwnerKeyAndRequestReuseTheExistingJob() {
        BaseContext.setCurrentId(9L);
        AiJobServiceImpl service = service();
        AiReportGenerateDTO dto = productReport("product-1");
        when(taskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, BizAnalysisTask.class).setId(41L);
            return 1;
        });
        AtomicReference<BizAiExecutionDetail> persistedDetail = new AtomicReference<>();
        when(executionDetailMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> persistedDetail.get());
        when(executionDetailMapper.insert(any(BizAiExecutionDetail.class))).thenAnswer(invocation -> {
            persistedDetail.set(invocation.getArgument(0, BizAiExecutionDetail.class));
            return 1;
        });

        AiJobCreatedVO created = service.createReportJob(dto, "submit-1");
        AiJobCreatedVO repeated = service.createReportJob(productReport("product-1"), "submit-1");

        assertThat(created.jobId()).isEqualTo(41L);
        assertThat(created.taskStatus()).isEqualTo("pending");
        assertThat(created.reused()).isFalse();
        assertThat(repeated.jobId()).isEqualTo(41L);
        assertThat(repeated.reused()).isTrue();
        verify(taskMapper).insert(any(BizAnalysisTask.class));
        verify(executionDetailMapper).insert(any(BizAiExecutionDetail.class));
        verify(executionService).submit(41L);
    }

    @Test
    void sameOwnerKeyWithDifferentCanonicalRequestIsRejected() {
        BaseContext.setCurrentId(9L);
        AiJobServiceImpl service = service();
        when(taskMapper.insert(any(BizAnalysisTask.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, BizAnalysisTask.class).setId(41L);
            return 1;
        });
        BizAiExecutionDetail existing = executionDetail(41L, "different-request");
        when(executionDetailMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, existing);

        service.createReportJob(productReport("product-1"), "submit-1");

        assertThatThrownBy(() -> service.createReportJob(productReport("product-2"), "submit-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("幂等键");
        verify(executionService).submit(41L);
        verify(taskMapper).insert(any(BizAnalysisTask.class));
    }

    @Test
    void reportJobRequiresExactlyOneTarget() {
        BaseContext.setCurrentId(9L);
        AiJobServiceImpl service = service();
        AiReportGenerateDTO dto = productReport("product-1");
        dto.setSellerId("seller-1");

        assertThatThrownBy(() -> service.createReportJob(dto, "submit-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("一个目标");
        verify(taskMapper, never()).insert(any(BizAnalysisTask.class));
    }

    private AiJobServiceImpl service() {
        return new AiJobServiceImpl(taskMapper, executionDetailMapper, executionService, cacheService, eventService,
                new ObjectMapper());
    }

    private AiReportGenerateDTO productReport(String productId) {
        AiReportGenerateDTO dto = new AiReportGenerateDTO();
        dto.setProductId(productId);
        dto.setLanguage("zh-CN");
        dto.setForceRefresh(false);
        return dto;
    }

    private BizAiExecutionDetail executionDetail(Long taskId, String requestHash) {
        BizAiExecutionDetail detail = new BizAiExecutionDetail();
        detail.setTaskId(taskId);
        detail.setRequestHash(requestHash);
        return detail;
    }
}
