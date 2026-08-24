package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "同步执行记录响应")
public class SyncExecutionVO {
    @Schema(description = "执行记录 ID")
    private Long id;
    @Schema(description = "同步配置 ID")
    private Long configId;
    @Schema(description = "同步名称")
    private String syncName;
    @Schema(description = "触发类型")
    private String triggerType;
    @Schema(description = "执行状态")
    private String executionStatus;
    @Schema(description = "关联任务 ID")
    private Long linkedTaskId;
    @Schema(description = "关联任务类型")
    private String linkedTaskType;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
