package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 文案生成参数")
public class AiContentGenerateDTO {
    @Schema(description = "目标类型", example = "product")
    private String targetType;
    @Schema(description = "目标 ID", example = "99a4788cb24856965c36a24e339b6058")
    private String targetId;
    @Schema(description = "文案类型", example = "product_title")
    private String contentType;
    @Schema(description = "文案风格", example = "simple")
    private String styleType = "simple";
    @Schema(description = "输出语言", example = "zh-CN")
    private String language = "zh-CN";
    @Schema(description = "额外生成要求", example = "突出物流快和性价比")
    private String extraRequirement;
}
