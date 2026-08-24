package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_crawl_task")
public class BizCrawlTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String platform;
    private String targetUrl;
    private String targetType;
    private String taskStatus;
    private Integer progress;
    private Integer maxCount;
    private Integer successCount;
    private Integer failCount;
    private Integer delaySeconds;
    private String requestParam;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
