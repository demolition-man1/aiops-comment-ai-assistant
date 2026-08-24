package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论分析任务创建参数")
public class AnalysisTaskCreateDTO {
    @Schema(description = "目标类型", example = "product", allowableValues = {"product", "seller"})
    private String targetType;
    @Schema(description = "目标 ID，商品 ID 或商家 ID", example = "99a4788cb24856965c36a24e339b6058")
    private String targetId;
    @Schema(description = "分析类型", example = "comment_analysis")
    private String analysisType = "comment_analysis";
}
