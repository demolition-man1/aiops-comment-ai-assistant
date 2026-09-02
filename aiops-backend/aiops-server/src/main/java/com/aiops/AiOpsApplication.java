package com.aiops;

import com.aiops.properties.AliyunOssProperties;
import com.aiops.properties.AiJobProperties;
import com.aiops.properties.AiRateLimitProperties;
import com.aiops.properties.CommentAiHybridProperties;
import com.aiops.properties.JwtProperties;
import com.aiops.properties.PythonServiceProperties;
import com.aiops.properties.TaskMaintenanceProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.aiops.mapper")
@EnableConfigurationProperties({
        AliyunOssProperties.class,
        PythonServiceProperties.class,
        JwtProperties.class,
        AiRateLimitProperties.class,
        CommentAiHybridProperties.class,
        TaskMaintenanceProperties.class,
        AiJobProperties.class
})
public class AiOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiOpsApplication.class, args);
    }
}
