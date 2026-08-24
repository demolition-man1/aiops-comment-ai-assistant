package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 运营报告生成参数")
public class AiReportGenerateDTO {
    @Schema(description = "商品 ID，生成商品报告时必填", example = "99a4788cb24856965c36a24e339b6058")
    private String productId;
    @Schema(description = "商家 ID，生成商家报告时必填", example = "7c67e1448b00f6e969d365cea6b010ab")
    private String sellerId;
    @Schema(description = "是否强制刷新，true 时绕过已有缓存", example = "false")
    private Boolean forceRefresh = false;
    @Schema(description = "输出语言", example = "zh-CN")
    private String language = "zh-CN";
}
