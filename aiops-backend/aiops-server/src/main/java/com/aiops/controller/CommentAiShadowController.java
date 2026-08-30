package com.aiops.controller;

import com.aiops.dto.CommentAiAnnotationDTO;
import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.CommentAiShadowService;
import com.aiops.vo.CommentAiEvaluationVO;
import com.aiops.vo.CommentAiShadowResultVO;
import com.aiops.vo.CommentAiShadowRunVO;
import com.aiops.vo.CommentAiShadowTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/analysis/ai-shadow")
@RequiredArgsConstructor
@Tag(name = "评论 AI Shadow", description = "评论规则与 AI 分析的隔离对照任务")
public class CommentAiShadowController {

    private final CommentAiShadowService commentAiShadowService;

    @PostMapping("/tasks")
    @Operation(summary = "创建评论 AI Shadow 任务", description = "创建任务后在后台执行，不修改原评论聚合结果")
    public Result<CommentAiShadowTaskVO> createTask(@Valid @RequestBody CommentAiShadowTaskDTO createDTO) {
        return Result.success(commentAiShadowService.createTask(createDTO));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询评论 AI Shadow 任务")
    public Result<CommentAiShadowTaskVO> getTask(@Parameter(description = "任务 ID") @PathVariable Long taskId) {
        return Result.success(commentAiShadowService.getTask(taskId));
    }

    @GetMapping("/runs")
    @Operation(summary = "分页查询评论 AI Shadow 运行记录")
    public Result<PageResult<CommentAiShadowRunVO>> pageRuns(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String runStatus) {
        return Result.success(commentAiShadowService.pageRuns(pageNum, pageSize, targetType, targetId, runStatus));
    }

    @GetMapping("/runs/{runId}")
    @Operation(summary = "查询评论 AI Shadow 运行详情")
    public Result<CommentAiShadowRunVO> getRun(@PathVariable Long runId) {
        return Result.success(commentAiShadowService.getRun(runId));
    }

    @GetMapping("/runs/{runId}/results")
    @Operation(summary = "分页查询 Shadow 样本与人工标注")
    public Result<PageResult<CommentAiShadowResultVO>> pageResults(
            @PathVariable Long runId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "all、annotated 或 unannotated") @RequestParam(defaultValue = "all") String annotationStatus) {
        return Result.success(commentAiShadowService.pageResults(runId, pageNum, pageSize, annotationStatus));
    }

    @PutMapping("/comments/{commentId}/annotation")
    @Operation(summary = "新增或更新评论人工标注")
    public Result<Void> upsertAnnotation(@PathVariable Long commentId,
                                         @Valid @RequestBody CommentAiAnnotationDTO annotationDTO) {
        commentAiShadowService.upsertAnnotation(commentId, annotationDTO);
        return Result.success();
    }

    @GetMapping("/runs/{runId}/evaluation")
    @Operation(summary = "计算评论 AI Shadow 质量与运行指标")
    public Result<CommentAiEvaluationVO> evaluateRun(@PathVariable Long runId) {
        return Result.success(commentAiShadowService.evaluateRun(runId));
    }
}
