package com.aiops.controller;

import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.result.Result;
import com.aiops.service.CommentAiShadowService;
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
import org.springframework.web.bind.annotation.RestController;

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
}
