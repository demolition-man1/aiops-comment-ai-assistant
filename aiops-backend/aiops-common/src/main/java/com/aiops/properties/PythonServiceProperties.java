package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.python")
public class PythonServiceProperties {

    private String baseUrl = "http://localhost:8000";
    private Integer connectTimeoutSeconds = 5;
    private Integer readTimeoutSeconds = 60;
}
