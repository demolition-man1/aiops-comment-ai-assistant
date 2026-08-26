package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "问题解决方案保存参数")
public class ProblemSolutionDTO {
    @Schema(description = "问题类型", example = "logistics")
    private String problemType;
    @Schema(description = "英文类目", example = "bed_bath_table")
    private String categoryNameEn;
    @Schema(description = "方案标题", example = "优化包装防护")
    private String solutionTitle;
    @Schema(description = "方案内容")
    private String solutionContent;
    @Schema(description = "关键词，逗号分隔", example = "package,box,damage")
    private String keywords;
    @Schema(description = "来源类型", example = "manual")
    private String sourceType = "manual";
    @Schema(description = "优先级，越大越靠前")
    private Integer priority = 0;
    @Schema(description = "启用状态，1 启用 0 停用")
    private Integer enabled = 1;
}
