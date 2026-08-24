package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.jwt")
public class JwtProperties {
    private String secret = "change-me-to-a-long-random-secret";
    private Long expireSeconds = 86400L;
}

