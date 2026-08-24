package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务状态响应")
public class TaskVO {
    @Schema(description = "任务 ID", example = "33")
    private Long taskId;
    @Schema(description = "任务状态", example = "processing")
    private String taskStatus;
    @Schema(description = "导入类型", example = "csv")
    private String importType;
    @Schema(description = "任务进度，0-100", example = "60")
    private Integer progress;
    @Schema(description = "导入总行数", example = "1000")
    private Integer importedRows;
    @Schema(description = "成功数量", example = "980")
    private Integer successCount;
    @Schema(description = "失败数量", example = "20")
    private Integer failCount;
    @Schema(description = "错误信息")
    private String errorMessage;
}
