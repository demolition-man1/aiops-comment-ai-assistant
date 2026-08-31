package com.aiops.service.impl;

import com.aiops.client.PythonAiClient;
import com.aiops.exception.BusinessException;
import com.aiops.vo.RagIndexStatusVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagKnowledgeServiceImplTest {

    @Mock
    private PythonAiClient pythonAiClient;

    @Test
    void statusMapsSafePythonIndexFields() {
        when(pythonAiClient.getRagStatus()).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "enabled", true,
                        "ready", true,
                        "state", "ready",
                        "documentCount", 12,
                        "problemSolutionCount", 7,
                        "historicalReplyCount", 5,
                        "reviewEvidenceCount", 31,
                        "embeddingModel", "local-model"
                )
        ));

        RagIndexStatusVO status = new RagKnowledgeServiceImpl(pythonAiClient).getStatus();

        assertThat(status.getState()).isEqualTo("ready");
        assertThat(status.getDocumentCount()).isEqualTo(12);
        assertThat(status.getHistoricalReplyCount()).isEqualTo(5);
        assertThat(status.getReviewEvidenceCount()).isEqualTo(31);
    }

    @Test
    void reindexConvertsPythonConflictIntoReadableBusinessConflict() {
        when(pythonAiClient.reindexRag()).thenThrow(HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> new RagKnowledgeServiceImpl(pythonAiClient).reindex())
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(409))
                .hasMessageContaining("重建");
    }
}
