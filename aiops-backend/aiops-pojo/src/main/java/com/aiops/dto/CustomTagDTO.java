package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "自定义标签保存参数")
public class CustomTagDTO {
    @Schema(description = "标签名称", example = "包装破损")
    private String tagName;
    @Schema(description = "标签分组", example = "售后问题")
    private String tagGroup;
    @Schema(description = "标签颜色", example = "#409EFF")
    private String color;
    @Schema(description = "标签说明")
    private String description;
    @Schema(description = "排序值，越大越靠前")
    private Integer sortOrder = 0;
    @Schema(description = "启用状态，1 启用 0 停用")
    private Integer enabled = 1;
}
