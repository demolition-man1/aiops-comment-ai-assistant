package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 调用统计概览")
public class AiCallLogOverviewVO {
    @Schema(description = "调用总数")
    private Long totalCalls;
    @Schema(description = "成功次数")
    private Long successCalls;
    @Schema(description = "失败次数")
    private Long failedCalls;
    @Schema(description = "成功率")
    private BigDecimal successRate;
    @Schema(description = "token 总量")
    private Long totalTokens;
    @Schema(description = "估算总成本")
    private BigDecimal totalCost;
    @Schema(description = "平均耗时毫秒")
    private Long avgLatencyMs;
}
