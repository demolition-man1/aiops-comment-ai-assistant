package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.task-maintenance")
public class TaskMaintenanceProperties {
    private Boolean enabled = true;
    private Integer intervalSeconds = 300;
    private Integer staleProcessingMinutes = 30;
    private Integer lockWaitSeconds = 1;
    private Integer lockLeaseSeconds = 60;
}
