package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {
    private Integer productCount;
    private Integer sellerCount;
    private Integer commentCount;
    private BigDecimal avgScore;
    private BigDecimal negativeRate;
}

