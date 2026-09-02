package com.aiops.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiJobEventService {

    SseEmitter subscribe(Long jobId, Long lastEventId);

    void publishStage(Long jobId, String stage, Integer progress);

    void publishTextDelta(Long jobId, String textDelta, Long deltaId);

    void publishTerminal(Long jobId);
}
