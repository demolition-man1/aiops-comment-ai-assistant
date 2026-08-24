package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.ai-rate-limit")
public class AiRateLimitProperties {
    private Boolean enabled = true;
    private Long capacity = 20L;
    private Long refillTokens = 20L;
    private Long refillPeriodSeconds = 60L;
}
