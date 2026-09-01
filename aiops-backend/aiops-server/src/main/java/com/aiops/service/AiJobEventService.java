package com.aiops.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiJobEventService {

    SseEmitter subscribe(Long jobId, Long lastEventId);

    void publishStage(Long jobId, String stage, Integer progress);

    void publishTerminal(Long jobId);
}
