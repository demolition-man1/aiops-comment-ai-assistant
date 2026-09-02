package com.aiops.service.impl;

import com.aiops.dto.AiCallLogQueryDTO;
import com.aiops.context.AiJobContext;
import com.aiops.entity.BizAiCallLog;
import com.aiops.mapper.BizAiCallLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCallLogServiceImplTest {

    @Mock
    private BizAiCallLogMapper aiCallLogMapper;

    private AiCallLogServiceImpl aiCallLogService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BizAiCallLog.class);
        aiCallLogService = new AiCallLogServiceImpl(aiCallLogMapper);
    }

    @AfterEach
    void tearDown() {
        AiJobContext.remove();
    }

    @Test
    void recordEstimatesCostAndTrimsFailureStatus() {
        AiJobContext.set(81L, "operation_report");
        aiCallLogService.record(1L, "report", "product", "product-a", 3L,
                "deepseek-chat", "failed", 2000, 1500L, "timeout");

        ArgumentCaptor<BizAiCallLog> captor = ArgumentCaptor.forClass(BizAiCallLog.class);
        verify(aiCallLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getCallStatus()).isEqualTo("failed");
        assertThat(captor.getValue().getEstimatedCost()).isEqualByComparingTo("0.004000");
        assertThat(captor.getValue().getLatencyMs()).isEqualTo(1500L);
        assertThat(captor.getValue().getJobId()).isEqualTo(81L);
    }

    @Test
    void overviewCalculatesSuccessRateTokenAndCost() {
        BizAiCallLog success = new BizAiCallLog();
        success.setCallStatus("success");
        success.setTokenUsage(1000);
        success.setEstimatedCost(new BigDecimal("0.002000"));
        success.setLatencyMs(100L);
        success.setQueueLatencyMs(50L);
        success.setTotalLatencyMs(180L);
        BizAiCallLog failed = new BizAiCallLog();
        failed.setCallStatus("failed");
        failed.setTokenUsage(500);
        failed.setEstimatedCost(new BigDecimal("0.001000"));
        failed.setLatencyMs(300L);
        failed.setQueueLatencyMs(150L);
        failed.setTotalLatencyMs(520L);
        when(aiCallLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(success, failed));

        var overview = aiCallLogService.overview(new AiCallLogQueryDTO());

        assertThat(overview.getTotalCalls()).isEqualTo(2);
        assertThat(overview.getSuccessCalls()).isEqualTo(1);
        assertThat(overview.getFailedCalls()).isEqualTo(1);
        assertThat(overview.getSuccessRate()).isEqualByComparingTo("50.00");
        assertThat(overview.getTotalTokens()).isEqualTo(1500);
        assertThat(overview.getTotalCost()).isEqualByComparingTo("0.003000");
        assertThat(overview.getAvgLatencyMs()).isEqualTo(200);
        assertThat(overview.getAvgQueueLatencyMs()).isEqualTo(100);
        assertThat(overview.getAvgTotalLatencyMs()).isEqualTo(350);
    }
}
