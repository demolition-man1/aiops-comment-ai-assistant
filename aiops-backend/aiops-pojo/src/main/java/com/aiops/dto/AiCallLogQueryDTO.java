package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 调用日志查询参数")
public class AiCallLogQueryDTO {
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "业务类型")
    private String businessType;
    @Schema(description = "调用状态：success/failed")
    private String callStatus;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "目标 ID")
    private String targetId;
}
