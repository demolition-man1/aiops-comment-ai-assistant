package com.aiops.service.aijob;

import com.aiops.dto.ProductCompareDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.service.AnalysisService;
import com.aiops.vo.ProductCompareReportVO;
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
class ProductCompareAiJobHandlerTest {

    @Mock
    private AnalysisService analysisService;

    @Test
    void runsTheExistingComparisonGenerationAndReturnsItsPointer() throws Exception {
        ProductCompareDTO dto = new ProductCompareDTO();
        dto.setLeftProductId("product-1");
        dto.setRightProductId("product-2");
        BizAnalysisTask task = new BizAnalysisTask();
        task.setTaskType("product_compare");
        task.setRequestParam(new ObjectMapper().writeValueAsString(dto));
        when(analysisService.compareProducts(any(ProductCompareDTO.class))).thenReturn(report(72L));

        AiJobExecutionResult result = new ProductCompareAiJobHandler(analysisService, new ObjectMapper()).execute(task);

        assertThat(result.resultType()).isEqualTo("product_compare");
        assertThat(result.resultId()).isEqualTo(72L);
        verify(analysisService).compareProducts(any(ProductCompareDTO.class));
    }

    private ProductCompareReportVO report(Long id) {
        return new ProductCompareReportVO(id, "product-1", "product-2", "metrics", "summary", "advantages",
                "risk", "suggestions", "model", LocalDateTime.now());
    }
}
