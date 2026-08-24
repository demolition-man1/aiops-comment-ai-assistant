package com.aiops.service.impl;

import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizProductMapper;
import com.aiops.service.DashboardService;
import com.aiops.service.ReportService;
import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.DashboardVO;
import com.aiops.vo.ProductVO;
import com.aiops.vo.ReportOverviewVO;
import com.aiops.vo.TrendItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DashboardService dashboardService;
    private final BizCommentMapper commentMapper;
    private final BizProductMapper productMapper;

    @Override
    public ReportOverviewVO overview() {
        DashboardOverviewVO overview = dashboardService.overview();
        Map<String, List<ProductVO>> ranks = productRank(10);
        return new ReportOverviewVO(overview.getProductCount(), overview.getSellerCount(),
                overview.getCommentCount(), overview.getAvgScore(), overview.getNegativeRate(),
                trends(), commentMapper.selectSentimentDistribution(null, null),
                commentMapper.selectProblemDistribution(null, null), ranks.get("highRiskProducts"),
                ranks.get("hotProducts"), ranks.get("topRatedProducts"));
    }

    @Override
    public List<TrendItemVO> trends() {
        return commentMapper.selectTrendDistribution(null, null);
    }

    @Override
    public DashboardVO distributions() {
        return new DashboardVO(commentMapper.selectScoreDistribution(null, null),
                commentMapper.selectSentimentDistribution(null, null),
                productMapper.selectCategoryDistribution(null, null),
                List.of(), commentMapper.selectNegativeKeywordFallback(null, null),
                commentMapper.selectProblemDistribution(null, null), List.of(), trends());
    }

    @Override
    public Map<String, List<ProductVO>> productRank(Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        Map<String, List<ProductVO>> result = new LinkedHashMap<>();
        result.put("hotProducts", productMapper.selectHotProducts(normalizedLimit));
        result.put("highRiskProducts", productMapper.selectHighRiskProducts(3, normalizedLimit));
        result.put("topRatedProducts", productMapper.selectTopRatedProducts(3, normalizedLimit));
        return result;
    }
}
