package com.aiops.service;

import com.aiops.vo.DashboardVO;
import com.aiops.vo.ProductVO;
import com.aiops.vo.ReportOverviewVO;
import com.aiops.vo.TrendItemVO;

import java.util.List;
import java.util.Map;

public interface ReportService {
    ReportOverviewVO overview();

    List<TrendItemVO> trends();

    DashboardVO distributions();

    Map<String, List<ProductVO>> productRank(Integer limit);
}
