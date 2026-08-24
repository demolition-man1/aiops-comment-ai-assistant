package com.aiops.service.impl;

import com.aiops.properties.AiRateLimitProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Bucket4jAiRateLimitServiceTest {

    @Test
    void rejectsCallsAfterPerUserBusinessCapacityIsConsumed() {
        AiRateLimitProperties properties = new AiRateLimitProperties();
        properties.setCapacity(2L);
        properties.setRefillTokens(2L);
        properties.setRefillPeriodSeconds(60L);

        Bucket4jAiRateLimitService service = new Bucket4jAiRateLimitService(properties);

        assertThat(service.tryConsume("content", 7L)).isTrue();
        assertThat(service.tryConsume("content", 7L)).isTrue();
        assertThat(service.tryConsume("content", 7L)).isFalse();
        assertThat(service.tryConsume("report", 7L)).isTrue();
        assertThat(service.tryConsume("content", 8L)).isTrue();
    }
}
