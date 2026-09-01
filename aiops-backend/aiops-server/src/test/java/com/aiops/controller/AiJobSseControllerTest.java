package com.aiops.controller;

import com.aiops.service.AiJobEventService;
import com.aiops.service.AiJobService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiJobSseControllerTest {

    @Test
    void streamUsesNoStoreHeadersAndPassesTheLastEventId() {
        AiJobService jobService = mock(AiJobService.class);
        AiJobEventService eventService = mock(AiJobEventService.class);
        SseEmitter emitter = new SseEmitter();
        when(eventService.subscribe(7L, 4L)).thenReturn(emitter);

        var response = new AiJobController(jobService, eventService).streamEvents(7L, 4L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst("X-Accel-Buffering")).isEqualTo("no");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM);
        assertThat(response.getBody()).isSameAs(emitter);
    }
}
