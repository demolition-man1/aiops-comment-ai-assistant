package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_task_record")
public class BizTaskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private String taskType;
    private String taskStatus;
    private Integer progress;
    private String sourceTable;
    private Long sourceId;
    private String targetType;
    private String targetId;
    private String requestParam;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
