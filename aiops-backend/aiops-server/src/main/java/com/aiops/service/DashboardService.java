package com.aiops.service;

import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.DashboardVO;

public interface DashboardService {
    DashboardOverviewVO overview();

    DashboardVO productDashboard(String productId);

    DashboardVO sellerDashboard(String sellerId);
}
