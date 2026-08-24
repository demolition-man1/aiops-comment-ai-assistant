package com.aiops.controller;

import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.TaskCenterService;
import com.aiops.vo.TaskRecordVO;
import com.aiops.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务中心", description = "统一查询导入、爬虫、分析和定时同步任务")
public class TaskCenterController {

    private final TaskCenterService taskCenterService;

    @GetMapping
    @Operation(summary = "分页查询任务列表", description = "聚合导入任务、爬虫任务、分析任务和定时同步记录")
    public Result<PageResult<TaskRecordVO>> pageTasks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
            @Parameter(description = "任务状态") @RequestParam(required = false) String taskStatus,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        return Result.success(taskCenterService.pageTasks(pageNum, pageSize, taskType, taskStatus, keyword));
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @Operation(summary = "导出任务 CSV", description = "按当前任务筛选条件导出导入、爬虫、分析和定时同步记录")
    public ResponseEntity<byte[]> exportTasks(
            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
            @Parameter(description = "任务状态") @RequestParam(required = false) String taskStatus,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aiops-tasks.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(taskCenterService.exportTasksCsv(taskType, taskStatus, keyword));
    }

    @GetMapping("/{recordKey}")
    @Operation(summary = "查询任务详情", description = "根据统一任务标识查询任务详情，如 analysis:1、crawler:2、sync:3")
    public Result<TaskRecordVO> getTask(@Parameter(description = "统一任务标识") @PathVariable String recordKey) {
        return Result.success(taskCenterService.getTask(recordKey));
    }

    @PostMapping("/{recordKey}/retry")
    @Operation(summary = "重试任务", description = "基于历史任务参数重新创建对应任务")
    public Result<TaskVO> retryTask(@Parameter(description = "统一任务标识") @PathVariable String recordKey) {
        return Result.success(taskCenterService.retryTask(recordKey));
    }
}
