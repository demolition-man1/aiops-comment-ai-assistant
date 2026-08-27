package com.aiops.service;

import com.aiops.vo.DashboardVO;
import com.aiops.vo.CategoryAnalysisVO;
import com.aiops.vo.ProductRankVO;
import com.aiops.vo.ReportOverviewVO;
import com.aiops.vo.TrendItemVO;

import java.util.List;

public interface ReportService {
    ReportOverviewVO overview();

    List<TrendItemVO> trends();

    DashboardVO distributions();

    ProductRankVO productRank(Integer limit);

    List<CategoryAnalysisVO> categoryAnalysis(Integer limit);

    byte[] exportOverviewCsv();
}
