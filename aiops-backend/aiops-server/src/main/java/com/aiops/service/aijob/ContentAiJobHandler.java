package com.aiops.service.aijob;

import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.service.AiService;
import com.aiops.vo.AiContentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentAiJobHandler implements AiJobHandler {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return "content";
    }

    @Override
    public AiJobExecutionResult execute(BizAnalysisTask task) {
        try {
            AiContentVO content = aiService.generateContent(
                    objectMapper.readValue(task.getRequestParam(), AiContentGenerateDTO.class));
            return new AiJobExecutionResult("ai_content", content.getContentId(), content.getModelName(),
                    null, null, null, null, null);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "AI 文案任务参数已损坏");
        }
    }
}
