package com.aiops.service.aijob;

import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.service.AiService;
import com.aiops.vo.NegativeReplyVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NegativeReplyAiJobHandlerTest {

    @Mock
    private AiService aiService;

    @Test
    void producesNegativeReplyResultPointerFromTheSavedReply() throws Exception {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setRequestParam(new ObjectMapper().writeValueAsString(new NegativeReplyGenerateDTO() {{
            setCommentId(17L);
            setToneType("sincere");
        }}));
        when(aiService.generateNegativeReply(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NegativeReplyVO(23L, 17L, null, null, null, null, null, null,
                        "model", null, 0, 0, false, java.util.List.of(), null, null));

        AiJobExecutionResult result = new NegativeReplyAiJobHandler(aiService, new ObjectMapper()).execute(task);

        assertThat(result.resultType()).isEqualTo("negative_reply");
        assertThat(result.resultId()).isEqualTo(23L);
        verify(aiService).generateNegativeReply(org.mockito.ArgumentMatchers.any());
    }
}
