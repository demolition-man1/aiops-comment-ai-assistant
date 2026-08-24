package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一任务记录响应")
public class TaskRecordVO {
    @Schema(description = "统一任务标识")
    private String recordKey;
    @Schema(description = "原始任务 ID")
    private Long sourceId;
    @Schema(description = "来源表")
    private String sourceTable;
    @Schema(description = "任务名称")
    private String taskName;
    @Schema(description = "任务类型")
    private String taskType;
    @Schema(description = "任务状态")
    private String taskStatus;
    @Schema(description = "任务进度")
    private Integer progress;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "目标 ID")
    private String targetId;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
