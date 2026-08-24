package com.aiops.service;

public interface AiRateLimitService {
    boolean tryConsume(String businessType, Long userId);
}
