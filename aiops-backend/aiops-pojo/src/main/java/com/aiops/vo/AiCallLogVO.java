package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 调用日志响应")
public class AiCallLogVO {
    @Schema(description = "日志 ID")
    private Long id;
    @Schema(description = "AI 任务 ID")
    private Long jobId;
    @Schema(description = "用户 ID")
    private Long userId;
    @Schema(description = "业务类型")
    private String businessType;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "目标 ID")
    private String targetId;
    @Schema(description = "Prompt 模板 ID")
    private Long promptTemplateId;
    @Schema(description = "模型名称")
    private String modelName;
    @Schema(description = "调用状态")
    private String callStatus;
    @Schema(description = "token 估算")
    private Integer tokenUsage;
    @Schema(description = "成本估算")
    private BigDecimal estimatedCost;
    @Schema(description = "耗时毫秒")
    private Long latencyMs;
    @Schema(description = "排队耗时毫秒")
    private Long queueLatencyMs;
    @Schema(description = "总耗时毫秒")
    private Long totalLatencyMs;
    @Schema(description = "安全失败分类")
    private String errorCode;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
