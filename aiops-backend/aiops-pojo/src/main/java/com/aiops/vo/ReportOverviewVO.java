package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据报表总览响应")
public class ReportOverviewVO {
    @Schema(description = "商品数量")
    private Integer productCount;
    @Schema(description = "商家数量")
    private Integer sellerCount;
    @Schema(description = "评论数量")
    private Integer commentCount;
    @Schema(description = "平均评分")
    private BigDecimal avgScore;
    @Schema(description = "负面占比")
    private BigDecimal negativeRate;
    @Schema(description = "评论趋势")
    private List<TrendItemVO> trendDistribution;
    @Schema(description = "情感分布")
    private List<DistributionItemVO> sentimentDistribution;
    @Schema(description = "差评问题分布")
    private List<DistributionItemVO> problemDistribution;
    @Schema(description = "高风险商品")
    private List<ProductVO> highRiskProducts;
    @Schema(description = "热门商品")
    private List<ProductVO> hotProducts;
    @Schema(description = "高评分商品")
    private List<ProductVO> topRatedProducts;
}
