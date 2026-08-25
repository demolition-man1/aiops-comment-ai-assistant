package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CSV 导入预检结果")
public class CsvImportPreflightVO {
    @Schema(description = "是否具备创建导入任务的基本条件")
    private Boolean ready;
    @Schema(description = "单 CSV 导入必填标准字段")
    private List<String> requiredFields;
    @Schema(description = "预计可导入数据行数")
    private Long estimatedRows;
    @Schema(description = "是否疑似重复导入")
    private Boolean duplicateLikely;
    @Schema(description = "重复导入提示")
    private String duplicateMessage;
    @Schema(description = "最近一次重复任务 ID")
    private Long lastTaskId;
}
