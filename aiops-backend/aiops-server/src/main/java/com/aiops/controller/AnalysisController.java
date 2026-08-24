package com.aiops.controller;

import com.aiops.dto.AnalysisTaskCreateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AnalysisService;
import com.aiops.vo.AnalysisResultVO;
import com.aiops.vo.ProductCompareReportVO;
import com.aiops.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "评论分析", description = "评论分析任务、分析结果和商品对比报告")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/tasks")
    @Operation(summary = "创建评论分析任务", description = "触发 Python 服务对商品或商家评论进行分析")
    public Result<TaskVO> createTask(@RequestBody AnalysisTaskCreateDTO createDTO) {
        return Result.success(analysisService.createAnalysisTask(createDTO));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询分析任务", description = "查询评论分析任务状态和进度")
    public Result<TaskVO> getTask(@Parameter(description = "任务 ID") @PathVariable Long taskId) {
        return Result.success(analysisService.getTask(taskId));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "商品分析结果", description = "查询指定商品最近一次评论分析结果")
    public Result<AnalysisResultVO> getProductAnalysis(@Parameter(description = "商品 ID") @PathVariable String productId) {
        return Result.success(analysisService.getProductAnalysis(productId));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "商家分析结果", description = "查询指定商家最近一次评论分析结果")
    public Result<AnalysisResultVO> getSellerAnalysis(@Parameter(description = "商家 ID") @PathVariable String sellerId) {
        return Result.success(analysisService.getSellerAnalysis(sellerId));
    }

    @PostMapping("/products/compare")
    @Operation(summary = "生成商品对比报告", description = "基于两个商品的分析结果生成 AI 对比报告")
    public Result<ProductCompareReportVO> compareProducts(@RequestBody ProductCompareDTO compareDTO) {
        return Result.success(analysisService.compareProducts(compareDTO));
    }

    @GetMapping("/products/compare")
    @Operation(summary = "分页查询商品对比报告", description = "查询历史商品对比报告")
    public Result<PageResult<ProductCompareReportVO>> pageProductCompareReports(
            @Parameter(description = "左侧商品 ID") @RequestParam(required = false) String leftProductId,
            @Parameter(description = "右侧商品 ID") @RequestParam(required = false) String rightProductId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(analysisService.pageProductCompareReports(leftProductId, rightProductId, pageNum, pageSize));
    }

    @GetMapping("/products/compare/{reportId}")
    @Operation(summary = "商品对比报告详情", description = "根据报告 ID 查询完整商品对比报告")
    public Result<ProductCompareReportVO> getProductCompareReport(@Parameter(description = "报告 ID") @PathVariable Long reportId) {
        return Result.success(analysisService.getProductCompareReport(reportId));
    }
}
