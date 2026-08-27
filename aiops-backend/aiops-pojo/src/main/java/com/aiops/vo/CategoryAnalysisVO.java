package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAnalysisVO {
    private String categoryName;
    private Integer productCount;
    private Integer commentCount;
    private BigDecimal avgScore;
    private Integer negativeCount;
    private BigDecimal negativeRate;
    private String topProblemType;
    private Integer topProblemCount;
    private String riskLevel;
}
