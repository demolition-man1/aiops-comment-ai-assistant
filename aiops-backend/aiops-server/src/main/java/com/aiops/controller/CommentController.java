package com.aiops.controller;

import com.aiops.dto.CommentQueryDTO;
import com.aiops.dto.CommentTagUpdateDTO;
import com.aiops.dto.CommentTranslateDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AiService;
import com.aiops.service.CommentService;
import com.aiops.vo.CommentTranslationVO;
import com.aiops.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "评论分页查询、负面评论筛选和人工标签维护")
public class CommentController {

    private final CommentService commentService;
    private final AiService aiService;

    @GetMapping
    @Operation(summary = "分页查询评论", description = "按商品、情感、差评类型等条件筛选评论")
    public Result<PageResult<CommentVO>> pageComments(@ModelAttribute CommentQueryDTO queryDTO) {
        return Result.success(commentService.pageComments(queryDTO));
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "评论详情", description = "根据评论主键查询评论详情")
    public Result<CommentVO> getComment(@Parameter(description = "评论主键 ID") @PathVariable Long commentId) {
        return Result.success(commentService.getComment(commentId));
    }

    @GetMapping("/negative")
    @Operation(summary = "分页查询负面评论", description = "只返回负面评论，用于差评处理和标签编辑")
    public Result<PageResult<CommentVO>> pageNegativeComments(@ModelAttribute CommentQueryDTO queryDTO) {
        return Result.success(commentService.pageNegativeComments(queryDTO));
    }

    @PutMapping("/{commentId}/tags")
    @Operation(summary = "更新评论标签", description = "人工修改单条评论的自定义标签和问题分类")
    public Result<CommentVO> updateTags(@Parameter(description = "评论主键 ID") @PathVariable Long commentId,
                                        @RequestBody CommentTagUpdateDTO updateDTO) {
        return Result.success(commentService.updateTags(commentId, updateDTO));
    }

    @PostMapping("/{commentId}/translate")
    @Operation(summary = "翻译评论原文", description = "按当前界面语言翻译单条评论，结果优先读取 Redis 缓存")
    public Result<CommentTranslationVO> translateComment(@Parameter(description = "评论主键 ID") @PathVariable Long commentId,
                                                        @RequestBody(required = false) CommentTranslateDTO translateDTO) {
        return Result.success(aiService.translateComment(commentId, translateDTO));
    }
}
