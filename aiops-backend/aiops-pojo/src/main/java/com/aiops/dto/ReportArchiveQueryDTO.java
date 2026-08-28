package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报告归档查询参数")
public class ReportArchiveQueryDTO {
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "目标类型：product/seller")
    private String targetType;
    @Schema(description = "目标 ID")
    private String targetId;
    @Schema(description = "关键词，匹配报告标题或目标 ID")
    private String keyword;
    @Schema(description = "归档状态：archived/restored")
    private String archiveStatus;
    @Schema(description = "归档开始时间，格式 yyyy-MM-dd HH:mm:ss")
    private String startTime;
    @Schema(description = "归档结束时间，格式 yyyy-MM-dd HH:mm:ss")
    private String endTime;
}
