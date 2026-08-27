package com.aiops.service.impl;

import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizProductMapper;
import com.aiops.service.CacheService;
import com.aiops.service.DashboardService;
import com.aiops.service.ReportService;
import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.DashboardVO;
import com.aiops.vo.CategoryAnalysisVO;
import com.aiops.vo.DistributionItemVO;
import com.aiops.vo.ProductRankVO;
import com.aiops.vo.ProductVO;
import com.aiops.vo.ReportOverviewVO;
import com.aiops.vo.TrendItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Duration REPORT_CACHE_TTL = Duration.ofMinutes(10);
    private static final String OVERVIEW_CACHE_KEY = "report:overview";
    private static final String DISTRIBUTIONS_CACHE_KEY = "report:distributions";
    private static final String PRODUCT_RANK_CACHE_PREFIX = "report:product-rank:";
    private static final String CATEGORY_ANALYSIS_CACHE_PREFIX = "report:category-analysis:";

    private final DashboardService dashboardService;
    private final BizCommentMapper commentMapper;
    private final BizProductMapper productMapper;
    private final CacheService cacheService;

    @Override
    public ReportOverviewVO overview() {
        return cacheService.get(OVERVIEW_CACHE_KEY, ReportOverviewVO.class)
                .orElseGet(() -> {
                    ReportOverviewVO result = buildOverview();
                    cacheService.set(OVERVIEW_CACHE_KEY, result, REPORT_CACHE_TTL);
                    return result;
                });
    }

    private ReportOverviewVO buildOverview() {
        DashboardOverviewVO overview = dashboardService.overview();
        ProductRankVO ranks = productRank(10);
        return new ReportOverviewVO(overview.getProductCount(), overview.getSellerCount(),
                overview.getCommentCount(), overview.getAvgScore(), overview.getNegativeRate(),
                trends(), commentMapper.selectSentimentDistribution(null, null),
                commentMapper.selectProblemDistribution(null, null), ranks.getHighRiskProducts(),
                ranks.getHotProducts(), ranks.getTopRatedProducts());
    }

    @Override
    public List<TrendItemVO> trends() {
        return commentMapper.selectTrendDistribution(null, null);
    }

    @Override
    public DashboardVO distributions() {
        return cacheService.get(DISTRIBUTIONS_CACHE_KEY, DashboardVO.class)
                .orElseGet(() -> {
                    DashboardVO result = new DashboardVO(commentMapper.selectScoreDistribution(null, null),
                            commentMapper.selectSentimentDistribution(null, null),
                            productMapper.selectCategoryDistribution(null, null),
                            List.of(), commentMapper.selectNegativeKeywordFallback(null, null),
                            commentMapper.selectProblemDistribution(null, null), List.of(), trends());
                    cacheService.set(DISTRIBUTIONS_CACHE_KEY, result, REPORT_CACHE_TTL);
                    return result;
                });
    }

    @Override
    public ProductRankVO productRank(Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        String cacheKey = PRODUCT_RANK_CACHE_PREFIX + normalizedLimit;
        return cacheService.get(cacheKey, ProductRankVO.class)
                .orElseGet(() -> {
                    ProductRankVO result = new ProductRankVO(
                            productMapper.selectHotProducts(normalizedLimit),
                            productMapper.selectHighRiskProducts(3, normalizedLimit),
                            productMapper.selectTopRatedProducts(3, normalizedLimit)
                    );
                    cacheService.set(cacheKey, result, REPORT_CACHE_TTL);
                    return result;
                });
    }

    @Override
    public List<CategoryAnalysisVO> categoryAnalysis(Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        String cacheKey = CATEGORY_ANALYSIS_CACHE_PREFIX + normalizedLimit;
        return cacheService.get(cacheKey, CategoryAnalysisVO[].class)
                .map(Arrays::asList)
                .orElseGet(() -> {
                    List<CategoryAnalysisVO> result = productMapper.selectCategoryAnalysis(normalizedLimit);
                    cacheService.set(cacheKey, result, REPORT_CACHE_TTL);
                    return result;
                });
    }

    @Override
    public byte[] exportOverviewCsv() {
        ReportOverviewVO report = overview();
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "[Overview]");
        appendLine(builder, "metric", "value");
        appendLine(builder, "productCount", report.getProductCount());
        appendLine(builder, "sellerCount", report.getSellerCount());
        appendLine(builder, "commentCount", report.getCommentCount());
        appendLine(builder, "avgScore", report.getAvgScore());
        appendLine(builder, "negativeRate", report.getNegativeRate());
        builder.append('\n');

        appendLine(builder, "[Trend]");
        appendLine(builder, "timeBucket", "commentCount", "negativeCount", "negativeRate", "avgScore");
        for (TrendItemVO item : safeList(report.getTrendDistribution())) {
            appendLine(builder, item.getTimeBucket(), item.getCommentCount(), item.getNegativeCount(),
                    item.getNegativeRate(), item.getAvgScore());
        }
        builder.append('\n');

        appendDistribution(builder, "[Sentiment]", report.getSentimentDistribution());
        appendDistribution(builder, "[Problem]", report.getProblemDistribution());
        appendProducts(builder, "[Hot Products]", report.getHotProducts());
        appendProducts(builder, "[High Risk Products]", report.getHighRiskProducts());
        appendProducts(builder, "[Top Rated Products]", report.getTopRatedProducts());
        return ("\uFEFF" + builder).getBytes(StandardCharsets.UTF_8);
    }

    private void appendDistribution(StringBuilder builder, String section, List<DistributionItemVO> items) {
        appendLine(builder, section);
        appendLine(builder, "name", "count");
        for (DistributionItemVO item : safeList(items)) {
            appendLine(builder, item.getName(), item.getCount());
        }
        builder.append('\n');
    }

    private void appendProducts(StringBuilder builder, String section, List<ProductVO> products) {
        appendLine(builder, section);
        appendLine(builder, "productId", "sellerId", "categoryNameEn", "avgPrice", "reviewCount", "avgScore",
                "negativeRate");
        for (ProductVO product : safeList(products)) {
            appendLine(builder, product.getProductId(), product.getSellerId(), product.getCategoryNameEn(),
                    product.getAvgPrice(), product.getReviewCount(), product.getAvgScore(),
                    product.getNegativeRate());
        }
        builder.append('\n');
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void appendLine(StringBuilder builder, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(csvValue(values[index]));
        }
        builder.append('\n');
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
