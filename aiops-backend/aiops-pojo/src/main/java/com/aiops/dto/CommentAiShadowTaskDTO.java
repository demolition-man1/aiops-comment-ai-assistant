package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "评论 AI Shadow 分析任务创建参数")
public class CommentAiShadowTaskDTO {
    @NotBlank(message = "目标类型不能为空")
    @Pattern(regexp = "product|seller", message = "目标类型仅支持 product 或 seller")
    @Schema(description = "目标类型", example = "product", allowableValues = {"product", "seller"})
    private String targetType;

    @NotBlank(message = "目标 ID 不能为空")
    @Size(max = 64, message = "目标 ID 长度不能超过 64")
    @Schema(description = "商品 ID 或商家 ID", example = "99a4788cb24856965c36a24e339b6058")
    private String targetId;

    @NotNull(message = "样本量不能为空")
    @Min(value = 1, message = "样本量至少为 1")
    @Max(value = 100, message = "样本量不能超过 100")
    @Schema(description = "抽样评论数", example = "60", defaultValue = "60")
    private Integer sampleSize = 60;

    @Schema(description = "可复现抽样种子", example = "20260829")
    @NotNull(message = "抽样种子不能为空")
    private Integer sampleSeed = 20260829;

    @NotNull(message = "Token 预算不能为空")
    @Min(value = 1000, message = "Token 预算至少为 1000")
    @Max(value = 100000, message = "Token 预算不能超过 100000")
    @Schema(description = "本次运行的 Token 软上限", example = "60000", defaultValue = "60000")
    private Integer maxTotalTokens = 60000;

    @NotBlank(message = "语言不能为空")
    @Pattern(regexp = "zh-CN|en-US|pt-BR", message = "语言仅支持 zh-CN、en-US 或 pt-BR")
    @Schema(description = "Prompt 模板语言", example = "zh-CN", allowableValues = {"zh-CN", "en-US", "pt-BR"})
    private String language = "zh-CN";
}
