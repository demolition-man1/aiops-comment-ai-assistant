package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerVO {
    private String sellerId;
    private String sellerState;
    private Integer productCount;
    private Integer orderCount;
    private BigDecimal avgScore;
    private BigDecimal negativeRate;
    private List<String> mainCategories;
}

