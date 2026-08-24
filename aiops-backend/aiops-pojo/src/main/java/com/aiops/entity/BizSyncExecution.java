package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_sync_execution")
public class BizSyncExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long configId;
    private String triggerType;
    private String executionStatus;
    private Long linkedTaskId;
    private String linkedTaskType;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
