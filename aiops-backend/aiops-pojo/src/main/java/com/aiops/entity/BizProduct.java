package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_product")
public class BizProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productId;
    private String sellerId;
    private String categoryName;
    private String categoryNameEn;
    private BigDecimal avgPrice;
    private BigDecimal avgFreight;
    private Integer orderCount;
    private Integer reviewCount;
    private BigDecimal avgScore;
    private BigDecimal negativeRate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
