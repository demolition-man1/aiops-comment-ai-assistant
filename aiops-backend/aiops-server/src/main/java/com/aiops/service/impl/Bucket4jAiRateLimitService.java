package com.aiops.service.impl;

import com.aiops.properties.AiRateLimitProperties;
import com.aiops.service.AiRateLimitService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class Bucket4jAiRateLimitService implements AiRateLimitService {

    private final AiRateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String businessType, Long userId) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return true;
        }
        return buckets.computeIfAbsent(bucketKey(businessType, userId), ignored -> newBucket())
                .tryConsume(1);
    }

    private Bucket newBucket() {
        long capacity = positive(properties.getCapacity(), 20L);
        long refillTokens = positive(properties.getRefillTokens(), capacity);
        long refillPeriodSeconds = positive(properties.getRefillPeriodSeconds(), 60L);
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.greedy(refillTokens, Duration.ofSeconds(refillPeriodSeconds)))
                .withInitialTokens(capacity);
        return Bucket.builder().addLimit(limit).build();
    }

    private String bucketKey(String businessType, Long userId) {
        String user = userId == null ? "anonymous" : String.valueOf(userId);
        String business = businessType == null || businessType.isBlank() ? "default" : businessType.trim();
        return business + ":" + user;
    }

    private long positive(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
