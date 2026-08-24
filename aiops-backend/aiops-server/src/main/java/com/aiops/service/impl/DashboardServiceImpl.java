package com.aiops.service.impl;

import com.aiops.converter.AnalysisJsonConverter;
import com.aiops.entity.BizCommentAnalysisResult;
import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizCommentAnalysisResultMapper;
import com.aiops.mapper.BizProductMapper;
import com.aiops.mapper.BizSellerMapper;
import com.aiops.service.DashboardService;
import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.DashboardVO;
import com.aiops.vo.KeywordItemVO;
import com.aiops.vo.TrendItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BizProductMapper productMapper;
    private final BizSellerMapper sellerMapper;
    private final BizCommentMapper commentMapper;
    private final BizCommentAnalysisResultMapper analysisResultMapper;
    private final AnalysisJsonConverter analysisJsonConverter;

    @Override
    public DashboardOverviewVO overview() {
        long productCount = productMapper.selectCount(null);
        long sellerCount = sellerMapper.selectCount(null);
        long commentCount = commentMapper.selectCount(null);
        long negativeCount = commentMapper.selectCount(new LambdaQueryWrapper<com.aiops.entity.BizComment>()
                .eq(com.aiops.entity.BizComment::getIsNegative, 1));
        BigDecimal avgScore = commentMapper.selectAverageReviewScore();
        BigDecimal negativeRate = commentCount == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(negativeCount).divide(BigDecimal.valueOf(commentCount), 4, java.math.RoundingMode.HALF_UP);
        return new DashboardOverviewVO((int) productCount, (int) sellerCount, (int) commentCount,
                avgScore == null ? BigDecimal.ZERO : avgScore, negativeRate);
    }

    @Override
    public DashboardVO productDashboard(String productId) {
        BizCommentAnalysisResult latest = latestAnalysis("product", productId);
        List<KeywordItemVO> negativeKeywords = latest == null
                ? commentMapper.selectNegativeKeywordFallback("product", productId)
                : analysisJsonConverter.parseKeywords(latest.getNegativeKeywords());
        List<TrendItemVO> trends = latest == null
                ? commentMapper.selectTrendDistribution("product", productId)
                : analysisJsonConverter.parseTrends(latest.getTrendDistribution());
        return new DashboardVO(commentMapper.selectScoreDistribution("product", productId),
                commentMapper.selectSentimentDistribution("product", productId),
                productMapper.selectCategoryDistribution(productId, null),
                latest == null ? List.of() : analysisJsonConverter.parseKeywords(latest.getTopKeywords()),
                negativeKeywords,
                commentMapper.selectProblemDistribution("product", productId),
                latest == null ? List.of() : analysisJsonConverter.parseDistributions(latest.getCustomTagDistribution()),
                trends);
    }

    @Override
    public DashboardVO sellerDashboard(String sellerId) {
        BizCommentAnalysisResult latest = latestAnalysis("seller", sellerId);
        List<KeywordItemVO> negativeKeywords = latest == null
                ? commentMapper.selectNegativeKeywordFallback("seller", sellerId)
                : analysisJsonConverter.parseKeywords(latest.getNegativeKeywords());
        List<TrendItemVO> trends = latest == null
                ? commentMapper.selectTrendDistribution("seller", sellerId)
                : analysisJsonConverter.parseTrends(latest.getTrendDistribution());
        return new DashboardVO(commentMapper.selectScoreDistribution("seller", sellerId),
                commentMapper.selectSentimentDistribution("seller", sellerId),
                productMapper.selectCategoryDistribution(null, sellerId),
                latest == null ? List.of() : analysisJsonConverter.parseKeywords(latest.getTopKeywords()),
                negativeKeywords,
                commentMapper.selectProblemDistribution("seller", sellerId),
                latest == null ? List.of() : analysisJsonConverter.parseDistributions(latest.getCustomTagDistribution()),
                trends);
    }

    private BizCommentAnalysisResult latestAnalysis(String targetType, String targetId) {
        return analysisResultMapper.selectOne(new LambdaQueryWrapper<BizCommentAnalysisResult>()
                .eq(BizCommentAnalysisResult::getTargetType, targetType)
                .eq(BizCommentAnalysisResult::getTargetId, targetId)
                .orderByDesc(BizCommentAnalysisResult::getCreateTime)
                .last("limit 1"));
    }
}
