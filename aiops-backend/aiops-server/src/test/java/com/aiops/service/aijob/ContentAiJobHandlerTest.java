package com.aiops.service.aijob;

import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.service.AiService;
import com.aiops.vo.AiContentVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentAiJobHandlerTest {

    @Mock
    private AiService aiService;

    @Test
    void producesContentResultPointerFromTheSavedContent() throws Exception {
        AiContentGenerateDTO dto = new AiContentGenerateDTO();
        dto.setTargetType("product");
        dto.setTargetId("p-1");
        dto.setContentType("product_title");
        BizAnalysisTask task = new BizAnalysisTask();
        task.setRequestParam(new ObjectMapper().writeValueAsString(dto));
        when(aiService.generateContent(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AiContentVO(31L, "content", "model"));

        AiJobExecutionResult result = new ContentAiJobHandler(aiService, new ObjectMapper()).execute(task);

        assertThat(result.resultType()).isEqualTo("ai_content");
        assertThat(result.resultId()).isEqualTo(31L);
        verify(aiService).generateContent(org.mockito.ArgumentMatchers.any());
    }
}
