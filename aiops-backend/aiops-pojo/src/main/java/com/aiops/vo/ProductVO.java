package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {
    private String productId;
    private String sellerId;
    private String categoryNameEn;
    private BigDecimal avgPrice;
    private Integer reviewCount;
    private BigDecimal avgScore;
    private BigDecimal negativeRate;
}

