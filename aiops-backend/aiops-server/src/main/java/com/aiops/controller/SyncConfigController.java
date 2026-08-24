package com.aiops.controller;

import com.aiops.dto.SyncConfigDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.SyncConfigService;
import com.aiops.vo.SyncConfigVO;
import com.aiops.vo.SyncExecutionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "定时同步", description = "同步配置、手动触发和同步执行记录")
public class SyncConfigController {

    private final SyncConfigService syncConfigService;

    @GetMapping("/configs")
    @Operation(summary = "分页查询同步配置", description = "按数据来源、启用状态筛选定时同步配置")
    public Result<PageResult<SyncConfigVO>> pageConfigs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "来源类型") @RequestParam(required = false) String sourceType,
            @Parameter(description = "启用状态：1 启用，0 停用") @RequestParam(required = false) Integer enabled) {
        return Result.success(syncConfigService.pageConfigs(pageNum, pageSize, sourceType, enabled));
    }

    @PostMapping("/configs")
    @Operation(summary = "创建同步配置", description = "创建 Olist 目录、CSV 文件或公开样例爬虫同步配置")
    public Result<SyncConfigVO> createConfig(@RequestBody SyncConfigDTO syncConfigDTO) {
        return Result.success(syncConfigService.createConfig(syncConfigDTO));
    }

    @PutMapping("/configs/{configId}")
    @Operation(summary = "修改同步配置", description = "更新同步来源、Cron 表达式、启用状态等参数")
    public Result<SyncConfigVO> updateConfig(@Parameter(description = "同步配置 ID") @PathVariable Long configId,
                                             @RequestBody SyncConfigDTO syncConfigDTO) {
        return Result.success(syncConfigService.updateConfig(configId, syncConfigDTO));
    }

    @PostMapping("/configs/{configId}/enable")
    @Operation(summary = "启用同步配置", description = "启用配置并注册 Quartz 定时任务")
    public Result<SyncConfigVO> enableConfig(@Parameter(description = "同步配置 ID") @PathVariable Long configId) {
        return Result.success(syncConfigService.enableConfig(configId));
    }

    @PostMapping("/configs/{configId}/disable")
    @Operation(summary = "停用同步配置", description = "停用配置并移除 Quartz 定时任务")
    public Result<SyncConfigVO> disableConfig(@Parameter(description = "同步配置 ID") @PathVariable Long configId) {
        return Result.success(syncConfigService.disableConfig(configId));
    }

    @PostMapping("/configs/{configId}/trigger")
    @Operation(summary = "立即触发同步", description = "手动执行一次同步配置，适合演示和排查")
    public Result<SyncExecutionVO> triggerNow(@Parameter(description = "同步配置 ID") @PathVariable Long configId) {
        return Result.success(syncConfigService.triggerNow(configId));
    }

    @GetMapping("/executions")
    @Operation(summary = "分页查询同步执行记录", description = "查看同步配置每次手动或定时执行结果")
    public Result<PageResult<SyncExecutionVO>> pageExecutions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "同步配置 ID") @RequestParam(required = false) Long configId,
            @Parameter(description = "执行状态") @RequestParam(required = false) String status) {
        return Result.success(syncConfigService.pageExecutions(pageNum, pageSize, configId, status));
    }
}
