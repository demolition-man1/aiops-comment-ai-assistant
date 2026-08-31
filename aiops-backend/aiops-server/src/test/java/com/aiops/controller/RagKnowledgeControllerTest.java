package com.aiops.controller;

import com.aiops.exception.BusinessException;
import com.aiops.service.RagKnowledgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagKnowledgeControllerTest {

    @Mock
    private RagKnowledgeService ragKnowledgeService;

    @Test
    void reindexReturnsHttpConflictWhenAnotherRebuildIsActive() {
        when(ragKnowledgeService.reindex()).thenThrow(new BusinessException(409, "知识索引正在重建"));

        var response = new RagKnowledgeController(ragKnowledgeService).reindex();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(409);
        assertThat(response.getBody().getMsg()).contains("重建");
    }
}
