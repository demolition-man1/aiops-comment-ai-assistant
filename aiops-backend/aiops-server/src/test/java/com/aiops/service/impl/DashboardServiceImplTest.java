package com.aiops.service.impl;

import com.aiops.converter.AnalysisJsonConverter;
import com.aiops.mapper.BizCommentAnalysisResultMapper;
import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizProductMapper;
import com.aiops.mapper.BizSellerMapper;
import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.DashboardVO;
import com.aiops.vo.KeywordItemVO;
import com.aiops.vo.TrendItemVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private BizProductMapper productMapper;

    @Mock
    private BizSellerMapper sellerMapper;

    @Mock
    private BizCommentMapper commentMapper;

    @Mock
    private BizCommentAnalysisResultMapper analysisResultMapper;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                productMapper,
                sellerMapper,
                commentMapper,
                analysisResultMapper,
                new AnalysisJsonConverter(new ObjectMapper())
        );
    }

    @Test
    void overviewUsesAverageReviewScoreFromComments() {
        when(productMapper.selectCount(null)).thenReturn(2L);
        when(sellerMapper.selectCount(null)).thenReturn(1L);
        when(commentMapper.selectCount(any())).thenReturn(4L, 1L);
        when(commentMapper.selectAverageReviewScore()).thenReturn(new BigDecimal("4.25"));

        DashboardOverviewVO result = dashboardService.overview();

        assertThat(result.getProductCount()).isEqualTo(2);
        assertThat(result.getSellerCount()).isEqualTo(1);
        assertThat(result.getCommentCount()).isEqualTo(4);
        assertThat(result.getAvgScore()).isEqualByComparingTo(new BigDecimal("4.25"));
        assertThat(result.getNegativeRate()).isEqualByComparingTo(new BigDecimal("0.2500"));
    }

    @Test
    void productDashboardFallsBackToRawCommentTrendAndNegativeKeywordsWhenAnalysisIsMissing() {
        when(analysisResultMapper.selectOne(any())).thenReturn(null);
        when(commentMapper.selectTrendDistribution("product", "p1")).thenReturn(List.of(
                new TrendItemVO("2018-06", 12, 3, new BigDecimal("25.00"), new BigDecimal("4.10"))
        ));
        when(commentMapper.selectNegativeKeywordFallback("product", "p1")).thenReturn(List.of(
                new KeywordItemVO("物流问题", 4)
        ));

        DashboardVO result = dashboardService.productDashboard("p1");

        assertThat(result.getTrendDistribution()).hasSize(1);
        assertThat(result.getTrendDistribution().get(0).getTimeBucket()).isEqualTo("2018-06");
        assertThat(result.getNegativeKeywordRank()).containsExactly(new KeywordItemVO("物流问题", 4));
    }
}
