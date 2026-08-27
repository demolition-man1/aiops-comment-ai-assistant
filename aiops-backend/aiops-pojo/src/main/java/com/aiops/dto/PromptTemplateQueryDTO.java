package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Prompt 模板查询参数")
public class PromptTemplateQueryDTO {
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "关键词，匹配模板名称或备注")
    private String keyword;
    @Schema(description = "业务类型")
    private String businessType;
    @Schema(description = "语言")
    private String language;
    @Schema(description = "启用状态，1 启用 0 停用")
    private Integer enabled;
}
