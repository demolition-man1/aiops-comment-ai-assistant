package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {
    private List<DistributionItemVO> scoreDistribution;
    private List<DistributionItemVO> sentimentDistribution;
    private List<DistributionItemVO> categoryDistribution;
    private List<KeywordItemVO> keywordRank;
    private List<KeywordItemVO> negativeKeywordRank;
    private List<DistributionItemVO> problemDistribution;
    private List<DistributionItemVO> customTagDistribution;
    private List<TrendItemVO> trendDistribution;
}
