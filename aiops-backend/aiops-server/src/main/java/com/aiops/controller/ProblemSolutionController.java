package com.aiops.controller;

import com.aiops.dto.ProblemSolutionDTO;
import com.aiops.dto.ProblemSolutionQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.ProblemSolutionService;
import com.aiops.vo.ProblemSolutionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problem-solutions")
@RequiredArgsConstructor
@Tag(name = "问题解决方案库", description = "沉淀评论问题对应的运营处理方案")
public class ProblemSolutionController {

    private final ProblemSolutionService problemSolutionService;

    @GetMapping
    @Operation(summary = "分页查询解决方案", description = "按问题类型、类目、关键词和启用状态筛选解决方案")
    public Result<PageResult<ProblemSolutionVO>> pageSolutions(@ModelAttribute ProblemSolutionQueryDTO queryDTO) {
        return Result.success(problemSolutionService.pageSolutions(queryDTO));
    }

    @GetMapping("/recommend")
    @Operation(summary = "推荐解决方案", description = "根据评论问题类型、商品类目和关键词推荐可复用方案")
    public Result<List<ProblemSolutionVO>> recommendSolutions(
            @Parameter(description = "问题类型") @RequestParam(required = false) String problemType,
            @Parameter(description = "英文类目") @RequestParam(required = false) String categoryNameEn,
            @Parameter(description = "关键词或评论内容") @RequestParam(required = false) String keyword) {
        return Result.success(problemSolutionService.recommendSolutions(problemType, categoryNameEn, keyword));
    }

    @PostMapping
    @Operation(summary = "创建解决方案", description = "新增一条问题处理方案")
    public Result<ProblemSolutionVO> createSolution(@RequestBody ProblemSolutionDTO problemSolutionDTO) {
        return Result.success(problemSolutionService.createSolution(problemSolutionDTO));
    }

    @PutMapping("/{solutionId}")
    @Operation(summary = "修改解决方案", description = "修改问题类型、类目、标题、内容、关键词和优先级")
    public Result<ProblemSolutionVO> updateSolution(@Parameter(description = "方案 ID") @PathVariable Long solutionId,
                                                    @RequestBody ProblemSolutionDTO problemSolutionDTO) {
        return Result.success(problemSolutionService.updateSolution(solutionId, problemSolutionDTO));
    }

    @PutMapping("/{solutionId}/status")
    @Operation(summary = "修改解决方案状态", description = "启用或停用一条问题处理方案")
    public Result<ProblemSolutionVO> updateStatus(@Parameter(description = "方案 ID") @PathVariable Long solutionId,
                                                  @Parameter(description = "启用状态，1 启用 0 停用")
                                                  @RequestParam Integer enabled) {
        return Result.success(problemSolutionService.updateStatus(solutionId, enabled));
    }
}
