package com.aiops.service.impl;

import com.aiops.mapper.BizCommentMapper;
import com.aiops.mapper.BizProductMapper;
import com.aiops.service.CacheService;
import com.aiops.service.DashboardService;
import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.ProductRankVO;
import com.aiops.vo.ProductVO;
import com.aiops.vo.ReportOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private BizCommentMapper commentMapper;

    @Mock
    private BizProductMapper productMapper;

    @Mock
    private CacheService cacheService;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(dashboardService, commentMapper, productMapper, cacheService);
    }

    @Test
    void overviewReturnsCachedReportWithoutQueryingDatabase() {
        ReportOverviewVO cached = new ReportOverviewVO(
                3, 2, 9, new BigDecimal("4.50"), new BigDecimal("0.1200"),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(cacheService.get("report:overview", ReportOverviewVO.class)).thenReturn(Optional.of(cached));

        ReportOverviewVO result = reportService.overview();

        assertThat(result).isSameAs(cached);
        verify(dashboardService, never()).overview();
        verify(productMapper, never()).selectHotProducts(any());
    }

    @Test
    void overviewCachesFreshReportWhenCacheMisses() {
        when(cacheService.get("report:overview", ReportOverviewVO.class)).thenReturn(Optional.empty());
        when(cacheService.get("report:product-rank:10", ProductRankVO.class)).thenReturn(Optional.empty());
        when(dashboardService.overview()).thenReturn(new DashboardOverviewVO(
                5, 4, 12, new BigDecimal("4.20"), new BigDecimal("0.2500")
        ));
        when(productMapper.selectHotProducts(10)).thenReturn(List.of(product("p-hot", 20)));
        when(productMapper.selectHighRiskProducts(3, 10)).thenReturn(List.of(product("p-risk", 9)));
        when(productMapper.selectTopRatedProducts(3, 10)).thenReturn(List.of(product("p-top", 11)));

        ReportOverviewVO result = reportService.overview();

        assertThat(result.getProductCount()).isEqualTo(5);
        assertThat(result.getHotProducts()).extracting(ProductVO::getProductId).containsExactly("p-hot");
        verify(cacheService).set(eq("report:product-rank:10"), any(ProductRankVO.class), any(Duration.class));
        verify(cacheService).set(eq("report:overview"), any(ReportOverviewVO.class), any(Duration.class));
    }

    @Test
    void exportOverviewCsvContainsBomAndReportSections() {
        when(cacheService.get("report:overview", ReportOverviewVO.class)).thenReturn(Optional.empty());
        when(cacheService.get("report:product-rank:10", ProductRankVO.class)).thenReturn(Optional.empty());
        when(dashboardService.overview()).thenReturn(new DashboardOverviewVO(
                1, 1, 2, new BigDecimal("4.00"), new BigDecimal("0.5000")
        ));
        when(productMapper.selectHotProducts(10)).thenReturn(List.of(product("p-hot", 20)));
        when(productMapper.selectHighRiskProducts(3, 10)).thenReturn(List.of(product("p-risk", 9)));
        when(productMapper.selectTopRatedProducts(3, 10)).thenReturn(List.of(product("p-top", 11)));

        byte[] csv = reportService.exportOverviewCsv();
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(content).contains("[Overview]")
                .contains("productCount,1")
                .contains("[Hot Products]")
                .contains("p-hot");
    }

    private ProductVO product(String productId, int reviewCount) {
        return new ProductVO(productId, "seller-1", "health_beauty",
                new BigDecimal("19.90"), reviewCount, new BigDecimal("4.80"), new BigDecimal("0.1000"));
    }
}
