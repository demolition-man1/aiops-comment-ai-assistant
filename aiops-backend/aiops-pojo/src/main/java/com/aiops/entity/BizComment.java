package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_comment")
public class BizComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reviewId;
    private String orderId;
    private String productId;
    private String sellerId;
    private Integer reviewScore;
    private String reviewTitle;
    private String reviewContent;
    private String cleanContent;
    private LocalDateTime reviewTime;
    private String sentiment;
    private BigDecimal sentimentScore;
    private String keywords;
    private String problemType;
    private String manualProblemType;
    private String customTags;
    private Integer isNegative;
    private LocalDateTime tagUpdateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
