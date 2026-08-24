package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultVO {
    private String targetType;
    private String targetId;
    private Integer totalCount;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private BigDecimal positiveRate;
    private BigDecimal negativeRate;
    private List<KeywordItemVO> topKeywords;
    private List<KeywordItemVO> negativeKeywords;
    private List<DistributionItemVO> scoreDistribution;
    private List<DistributionItemVO> problemDistribution;
    private List<DistributionItemVO> customTagDistribution;
    private List<TrendItemVO> trendDistribution;
    private String summary;
    private LocalDateTime createTime;
}
