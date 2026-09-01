package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 异步任务创建响应")
public record AiJobCreatedVO(
        @Schema(description = "AI 任务 ID", example = "42") Long jobId,
        @Schema(description = "任务状态", example = "pending") String taskStatus,
        @Schema(description = "是否复用已有幂等任务", example = "false") boolean reused) {
}
