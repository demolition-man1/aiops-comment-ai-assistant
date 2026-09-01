package com.aiops.service.aijob;

import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.exception.BusinessException;
import com.aiops.service.AiService;
import com.aiops.vo.NegativeReplyVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NegativeReplyAiJobHandler implements AiJobHandler {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return "negative_reply";
    }

    @Override
    public AiJobExecutionResult execute(BizAnalysisTask task) {
        try {
            NegativeReplyVO reply = aiService.generateNegativeReply(
                    objectMapper.readValue(task.getRequestParam(), NegativeReplyGenerateDTO.class));
            return new AiJobExecutionResult("negative_reply", reply.getReplyId(), reply.getModelName(),
                    null, null, null, null, null);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "差评回复任务参数已损坏");
        }
    }
}
