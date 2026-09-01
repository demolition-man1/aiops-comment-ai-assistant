package com.aiops.controller;

import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AiJobService;
import com.aiops.vo.AiJobCreatedVO;
import com.aiops.vo.AiJobVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/jobs")
@RequiredArgsConstructor
@Tag(name = "AI 任务", description = "可持久化的 AI 报告与商品对比任务")
public class AiJobController {

    private final AiJobService aiJobService;

    @PostMapping("/reports")
    @Operation(summary = "提交运营报告任务")
    public Result<AiJobCreatedVO> createReportJob(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AiReportGenerateDTO dto) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Result.error(400, "Idempotency-Key 不能为空");
        }
        return Result.success(aiJobService.createReportJob(dto, idempotencyKey));
    }

    @PostMapping("/product-comparisons")
    @Operation(summary = "提交商品对比任务")
    public Result<AiJobCreatedVO> createProductCompareJob(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ProductCompareDTO dto) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Result.error(400, "Idempotency-Key 不能为空");
        }
        return Result.success(aiJobService.createProductCompareJob(dto, idempotencyKey));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "查询 AI 任务")
    public Result<AiJobVO> getJob(@Parameter(description = "AI 任务 ID") @PathVariable Long jobId) {
        return Result.success(aiJobService.getOwnedJob(jobId));
    }

    @GetMapping
    @Operation(summary = "分页查询当前用户的 AI 任务")
    public Result<PageResult<AiJobVO>> pageJobs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String taskStatus) {
        return Result.success(aiJobService.pageOwnedJobs(pageNum, pageSize, jobType, taskStatus));
    }
}
