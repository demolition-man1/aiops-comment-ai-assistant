package com.aiops.service.aijob;

import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.service.AiService;
import com.aiops.vo.OperationReportVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAiJobHandlerTest {

    @Mock
    private AiService aiService;

    @Test
    void runsTheExistingProductReportGenerationAndReturnsItsPointer() throws Exception {
        AiReportGenerateDTO dto = new AiReportGenerateDTO();
        dto.setProductId("product-1");
        BizAnalysisTask task = new BizAnalysisTask();
        task.setTaskType("operation_report");
        task.setRequestParam(new ObjectMapper().writeValueAsString(dto));
        when(aiService.generateProductReport(any(AiReportGenerateDTO.class))).thenReturn(report(71L));

        AiJobExecutionResult result = new ReportAiJobHandler(aiService, new ObjectMapper()).execute(task);

        assertThat(result.resultType()).isEqualTo("operation_report");
        assertThat(result.resultId()).isEqualTo(71L);
        verify(aiService).generateProductReport(any(AiReportGenerateDTO.class));
    }

    private OperationReportVO report(Long id) {
        return new OperationReportVO(id, "product", "product-1", "title", "pain", "advantage", "risk",
                "operation", "copy", "service", "full", "model", LocalDateTime.now(), java.util.List.of());
    }
}
