package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Prompt 模板保存参数")
public class PromptTemplateDTO {
    @Schema(description = "模板名称", example = "默认差评回复模板")
    private String templateName;
    @Schema(description = "业务类型：report/content/negative_reply/translation/product_compare", example = "negative_reply")
    private String businessType;
    @Schema(description = "语言", example = "zh-CN")
    private String language = "zh-CN";
    @Schema(description = "模板内容，变量使用 {variableName} 写法")
    private String templateContent;
    @Schema(description = "变量说明 JSON", example = "[\"commentContent\",\"language\"]")
    private String variableSchema;
    @Schema(description = "是否默认模板，1 是 0 否")
    private Integer defaultFlag = 0;
    @Schema(description = "启用状态，1 启用 0 停用")
    private Integer enabled = 1;
    @Schema(description = "备注")
    private String remark;
}
