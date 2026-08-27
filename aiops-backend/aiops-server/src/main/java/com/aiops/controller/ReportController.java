package com.aiops.controller;

import com.aiops.result.Result;
import com.aiops.service.ReportService;
import com.aiops.vo.CategoryAnalysisVO;
import com.aiops.vo.DashboardVO;
import com.aiops.vo.ProductRankVO;
import com.aiops.vo.ReportOverviewVO;
import com.aiops.vo.TrendItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "数据报表", description = "全局运营报表、趋势分布和商品排行")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    @Operation(summary = "报表总览", description = "返回全局运营指标、趋势、分布和核心商品排行")
    public Result<ReportOverviewVO> overview() {
        return Result.success(reportService.overview());
    }

    @GetMapping("/trends")
    @Operation(summary = "评论趋势", description = "返回按月聚合的评论量、负面评论数、负面率和平均评分")
    public Result<List<TrendItemVO>> trends() {
        return Result.success(reportService.trends());
    }

    @GetMapping("/distributions")
    @Operation(summary = "统计分布", description = "返回评分、情感、类目、差评问题和关键词分布")
    public Result<DashboardVO> distributions() {
        return Result.success(reportService.distributions());
    }

    @GetMapping("/product-rank")
    @Operation(summary = "商品排行", description = "返回热门商品、高风险商品和高评分商品列表")
    public Result<ProductRankVO> productRank(
            @Parameter(description = "排行数量") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(reportService.productRank(limit));
    }

    @GetMapping("/categories")
    @Operation(summary = "类目聚合分析", description = "按商品类目聚合商品数、评论数、平均评分、负面占比和主要差评问题")
    public Result<List<CategoryAnalysisVO>> categoryAnalysis(
            @Parameter(description = "返回类目数量，最大 100") @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(reportService.categoryAnalysis(limit));
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @Operation(summary = "导出报表 CSV", description = "导出全局运营总览、趋势、分布和商品排行，适合 Excel 打开")
    public ResponseEntity<byte[]> exportOverview() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aiops-report.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(reportService.exportOverviewCsv());
    }
}
