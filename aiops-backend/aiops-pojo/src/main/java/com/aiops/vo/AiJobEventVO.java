package com.aiops.vo;

import java.time.LocalDateTime;

public record AiJobEventVO(
        Long eventId,
        String eventType,
        Long jobId,
        String jobType,
        String taskStatus,
        String jobStage,
        Integer progress,
        String resultType,
        Long resultId,
        LocalDateTime occurredAt) {
}
