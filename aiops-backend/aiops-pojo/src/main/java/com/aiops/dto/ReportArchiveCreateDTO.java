package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报告归档参数")
public class ReportArchiveCreateDTO {
    @Schema(description = "归档备注", example = "2026 年 8 月运营复盘")
    private String remark;
}
