package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_product_compare_report")
public class BizProductCompareReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String leftProductId;
    private String rightProductId;
    private String metricSnapshot;
    private String compareSummary;
    private String advantageAnalysis;
    private String riskAnalysis;
    private String operationSuggestions;
    private String modelName;
    private LocalDateTime createTime;
}
