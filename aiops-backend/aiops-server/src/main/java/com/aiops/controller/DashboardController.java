package com.aiops.controller;

import com.aiops.result.Result;
import com.aiops.service.DashboardService;
import com.aiops.vo.DashboardOverviewVO;
import com.aiops.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "数据看板", description = "首页概览、商品看板和商家看板")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "首页运营概览", description = "返回商品数、商家数、评论数、平均评分和负面占比")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "商品评论看板", description = "返回指定商品的趋势、情感和关键词统计")
    public Result<DashboardVO> productDashboard(@Parameter(description = "商品 ID") @PathVariable String productId) {
        return Result.success(dashboardService.productDashboard(productId));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "商家评论看板", description = "返回指定商家的趋势、情感和关键词统计")
    public Result<DashboardVO> sellerDashboard(@Parameter(description = "商家 ID") @PathVariable String sellerId) {
        return Result.success(dashboardService.sellerDashboard(sellerId));
    }
}
