package com.aiops.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.aliyun.oss")
public class AliyunOssProperties {

    private String endpoint;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
    private String uploadDir = "aiops/csv";
    private Long signedUrlExpireSeconds = 3600L;
}

