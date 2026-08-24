package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品排行响应")
public class ProductRankVO {
    @Schema(description = "热门商品")
    private List<ProductVO> hotProducts;
    @Schema(description = "高风险商品")
    private List<ProductVO> highRiskProducts;
    @Schema(description = "高评分商品")
    private List<ProductVO> topRatedProducts;
}
