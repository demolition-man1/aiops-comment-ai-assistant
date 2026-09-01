package com.aiops.controller;

import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.service.AiJobService;
import com.aiops.vo.AiJobCreatedVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobControllerTest {

    @Mock
    private AiJobService aiJobService;

    @Test
    void reportSubmissionRequiresIdempotencyKey() {
        AiReportGenerateDTO dto = new AiReportGenerateDTO();
        dto.setProductId("product-1");

        var response = new AiJobController(aiJobService).createReportJob(null, dto);

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMsg()).contains("Idempotency-Key");
    }

    @Test
    void reportSubmissionReturnsTheCreatedPendingJob() {
        AiReportGenerateDTO dto = new AiReportGenerateDTO();
        dto.setProductId("product-1");
        when(aiJobService.createReportJob(dto, "request-1"))
                .thenReturn(new AiJobCreatedVO(42L, "pending", false));

        var response = new AiJobController(aiJobService).createReportJob("request-1", dto);

        assertThat(response.getData().jobId()).isEqualTo(42L);
        verify(aiJobService).createReportJob(dto, "request-1");
    }
}
