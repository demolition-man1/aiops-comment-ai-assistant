package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_seller")
public class BizSeller {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sellerId;
    private String sellerCity;
    private String sellerState;
    private Integer productCount;
    private Integer orderCount;
    private BigDecimal avgScore;
    private BigDecimal negativeRate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
