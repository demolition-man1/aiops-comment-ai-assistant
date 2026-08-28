package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报告归档状态参数")
public class ReportArchiveStatusDTO {
    @Schema(description = "归档状态：archived/restored", example = "restored")
    private String archiveStatus;
}
