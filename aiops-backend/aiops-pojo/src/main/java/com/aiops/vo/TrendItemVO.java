package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendItemVO {
    private String timeBucket;
    private Integer commentCount;
    private Integer negativeCount;
    private BigDecimal negativeRate;
    private BigDecimal avgScore;
}
