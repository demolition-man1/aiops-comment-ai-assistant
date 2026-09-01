package com.aiops.service.aijob;

import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.service.AiService;
import com.aiops.vo.OperationReportVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportAiJobHandler implements AiJobHandler {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return "operation_report";
    }

    @Override
    public AiJobExecutionResult execute(BizAnalysisTask task) {
        AiReportGenerateDTO dto = readRequest(task);
        OperationReportVO report = "seller".equals(task.getTargetType())
                ? aiService.generateSellerReport(dto)
                : aiService.generateProductReport(dto);
        return new AiJobExecutionResult("operation_report", report.getReportId(), report.getModelName(),
                null, null, null, null, null);
    }

    private AiReportGenerateDTO readRequest(BizAnalysisTask task) {
        try {
            return objectMapper.readValue(task.getRequestParam(), AiReportGenerateDTO.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "运营报告任务参数已损坏");
        }
    }
}
