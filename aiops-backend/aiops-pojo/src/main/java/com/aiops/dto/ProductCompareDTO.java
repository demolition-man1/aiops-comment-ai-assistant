package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品对比分析参数")
public class ProductCompareDTO {
    @Schema(description = "左侧商品 ID", example = "99a4788cb24856965c36a24e339b6058")
    private String leftProductId;
    @Schema(description = "右侧商品 ID", example = "aca2eb7d00ea1a7b8ebd4e68314663af")
    private String rightProductId;
    @Schema(description = "输出语言", example = "zh-CN")
    private String language = "zh-CN";
    @Schema(description = "是否强制刷新，true 时重新调用 AI", example = "false")
    private Boolean forceRefresh = false;
}
