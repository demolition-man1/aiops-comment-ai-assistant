package com.aiops.service.aijob;

import com.aiops.dto.ProductCompareDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.service.AnalysisService;
import com.aiops.vo.ProductCompareReportVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCompareAiJobHandler implements AiJobHandler {

    private final AnalysisService analysisService;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return "product_compare";
    }

    @Override
    public AiJobExecutionResult execute(BizAnalysisTask task) {
        ProductCompareDTO dto = readRequest(task);
        ProductCompareReportVO report = analysisService.compareProducts(dto);
        return new AiJobExecutionResult("product_compare", report.getReportId(), report.getModelName(),
                null, null, null, null, null);
    }

    private ProductCompareDTO readRequest(BizAnalysisTask task) {
        try {
            return objectMapper.readValue(task.getRequestParam(), ProductCompareDTO.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "商品对比任务参数已损坏");
        }
    }
}
