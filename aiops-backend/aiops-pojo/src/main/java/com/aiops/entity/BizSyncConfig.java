package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_sync_config")
public class BizSyncConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String syncName;
    private String sourceType;
    private String dataSource;
    private String importMode;
    private String dataPath;
    private Long fileId;
    private String objectKey;
    private String fileUrl;
    private String platform;
    private String targetUrl;
    private String targetType;
    private Integer maxCount;
    private Integer delaySeconds;
    private String cronExpression;
    private Integer autoAnalysis;
    private Integer enabled;
    private String remark;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
