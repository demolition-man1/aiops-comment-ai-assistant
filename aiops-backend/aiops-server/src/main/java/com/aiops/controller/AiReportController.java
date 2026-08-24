package com.aiops.controller;

import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AiService;
import com.aiops.vo.OperationReportVO;
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
@RequestMapping("/api/ai/reports")
@RequiredArgsConstructor
@Tag(name = "AI 运营报告", description = "商品和商家评论分析报告生成与查询")
public class AiReportController {

    private final AiService aiService;

    @PostMapping("/product")
    @Operation(summary = "生成商品运营报告", description = "基于商品评论分析结果生成 AI 运营建议报告")
    public Result<OperationReportVO> generateProductReport(@RequestBody AiReportGenerateDTO generateDTO) {
        return Result.success(aiService.generateProductReport(generateDTO));
    }

    @PostMapping("/seller")
    @Operation(summary = "生成商家运营报告", description = "基于商家评论分析结果生成 AI 运营建议报告")
    public Result<OperationReportVO> generateSellerReport(@RequestBody AiReportGenerateDTO generateDTO) {
        return Result.success(aiService.generateSellerReport(generateDTO));
    }

    @GetMapping
    @Operation(summary = "分页查询运营报告", description = "查询历史生成的 AI 运营报告")
    public Result<PageResult<OperationReportVO>> pageReports(
            @Parameter(description = "目标类型，product/seller") @RequestParam(required = false) String targetType,
            @Parameter(description = "目标 ID") @RequestParam(required = false) String targetId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(aiService.pageReports(targetType, targetId, pageNum, pageSize));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "运营报告详情", description = "根据报告 ID 查询完整 AI 运营报告")
    public Result<OperationReportVO> getReport(@Parameter(description = "报告 ID") @PathVariable Long reportId) {
        return Result.success(aiService.getReport(reportId));
    }
}
