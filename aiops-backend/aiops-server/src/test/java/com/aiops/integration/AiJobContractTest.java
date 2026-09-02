package com.aiops.integration;

import com.aiops.vo.AiJobEventVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AiJobContractTest {

    @Test
    void serializesTextDeltaEventsWithFrontendFieldNames() throws Exception {
        AiJobEventVO event = new AiJobEventVO(
                7L, "text_delta", 9L, "negative_reply", "processing", "generating", 55,
                null, null, LocalDateTime.of(2026, 9, 2, 12, 0), "Thank you", 3L);

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(event);

        assertThat(json).contains("\"eventType\":\"text_delta\"")
                .contains("\"jobId\":9")
                .contains("\"textDelta\":\"Thank you\"")
                .contains("\"deltaId\":3");
    }
}
