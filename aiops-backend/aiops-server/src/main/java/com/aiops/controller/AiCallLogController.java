package com.aiops.controller;

import com.aiops.dto.AiCallLogQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AiCallLogService;
import com.aiops.vo.AiCallLogOverviewVO;
import com.aiops.vo.AiCallLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/call-logs")
@RequiredArgsConstructor
@Tag(name = "AI 调用日志", description = "查看 AI 调用记录、耗时、token 和成本估算")
public class AiCallLogController {

    private final AiCallLogService aiCallLogService;

    @GetMapping
    @Operation(summary = "分页查询 AI 调用日志", description = "按业务类型、状态和目标对象筛选 AI 调用日志")
    public Result<PageResult<AiCallLogVO>> pageLogs(@ModelAttribute AiCallLogQueryDTO queryDTO) {
        return Result.success(aiCallLogService.pageLogs(queryDTO));
    }

    @GetMapping("/overview")
    @Operation(summary = "AI 调用统计概览", description = "统计调用次数、成功率、token、估算成本和平均耗时")
    public Result<AiCallLogOverviewVO> overview(@ModelAttribute AiCallLogQueryDTO queryDTO) {
        return Result.success(aiCallLogService.overview(queryDTO));
    }
}
