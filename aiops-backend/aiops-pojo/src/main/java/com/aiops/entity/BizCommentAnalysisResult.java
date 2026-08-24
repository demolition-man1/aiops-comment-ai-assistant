package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_comment_analysis_result")
public class BizCommentAnalysisResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String targetType;
    private String targetId;
    private Integer totalCount;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private BigDecimal positiveRate;
    private BigDecimal negativeRate;
    private String topKeywords;
    private String negativeKeywords;
    private String problemDistribution;
    private String scoreDistribution;
    private String customTagDistribution;
    private String trendDistribution;
    private String summary;
    private LocalDateTime createTime;
}
