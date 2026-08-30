package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
@Schema(description = "评论 AI Hybrid 启用确认参数")
public class CommentAiHybridActivationDTO {
    @AssertTrue(message = "必须确认启用 Hybrid 问题分类")
    @Schema(description = "显式确认启用", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean confirmed;
}
