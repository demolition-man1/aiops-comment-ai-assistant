package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Prompt 模板响应")
public class PromptTemplateVO {
    @Schema(description = "模板 ID")
    private Long id;
    @Schema(description = "模板名称")
    private String templateName;
    @Schema(description = "业务类型")
    private String businessType;
    @Schema(description = "语言")
    private String language;
    @Schema(description = "模板内容")
    private String templateContent;
    @Schema(description = "变量说明 JSON")
    private String variableSchema;
    @Schema(description = "是否默认")
    private Integer defaultFlag;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
