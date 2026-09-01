package com.aiops.service.aijob;

public record AiJobExecutionResult(
        String resultType,
        Long resultId,
        String modelName,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Boolean tokenUsageEstimated,
        Long providerLatencyMs) {
}
