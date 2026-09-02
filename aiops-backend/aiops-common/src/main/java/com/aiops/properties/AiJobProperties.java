package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.ai-job")
public class AiJobProperties {
    private Integer corePoolSize = 2;
    private Integer maxPoolSize = 4;
    private Integer queueCapacity = 100;
    private Integer globalConcurrency = 4;
    private Integer perUserConcurrency = 2;
    private Integer leaseSeconds = 45;
    private Integer heartbeatSeconds = 15;
    private Integer timeoutSeconds = 120;
    private Boolean recoveryEnabled = true;
    private Integer recoveryIntervalSeconds = 60;

    public void validate() {
        if (positive(corePoolSize) == null || positive(maxPoolSize) == null || maxPoolSize < corePoolSize
                || positive(queueCapacity) == null || positive(globalConcurrency) == null
                || positive(perUserConcurrency) == null || positive(leaseSeconds) == null
                || positive(heartbeatSeconds) == null || heartbeatSeconds >= leaseSeconds
                || positive(timeoutSeconds) == null || timeoutSeconds < leaseSeconds
                || positive(recoveryIntervalSeconds) == null) {
            throw new IllegalStateException("Invalid AI job execution configuration");
        }
    }

    private Integer positive(Integer value) {
        return value != null && value > 0 ? value : null;
    }
}
