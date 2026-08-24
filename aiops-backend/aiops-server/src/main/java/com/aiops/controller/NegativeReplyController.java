package com.aiops.controller;

import com.aiops.dto.NegativeReplyEffectDTO;
import com.aiops.dto.NegativeReplyFavoriteDTO;
import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AiService;
import com.aiops.vo.NegativeReplyVO;
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
@RequestMapping("/api/ai/negative-replies")
@RequiredArgsConstructor
@Tag(name = "差评回复", description = "AI 差评回复生成、使用记录和效果跟踪")
public class NegativeReplyController {

    private final AiService aiService;

    @PostMapping
    @Operation(summary = "生成差评回复", description = "根据单条评论内容生成个性化差评回复")
    public Result<NegativeReplyVO> generateNegativeReply(@RequestBody NegativeReplyGenerateDTO generateDTO) {
        return Result.success(aiService.generateNegativeReply(generateDTO));
    }

    @GetMapping
    @Operation(summary = "分页查询差评回复", description = "查询历史生成的差评回复和效果标记")
    public Result<PageResult<NegativeReplyVO>> pageNegativeReplies(
            @Parameter(description = "商品 ID") @RequestParam(required = false) String productId,
            @Parameter(description = "商家 ID") @RequestParam(required = false) String sellerId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(aiService.pageNegativeReplies(productId, sellerId, pageNum, pageSize));
    }

    @PostMapping("/{replyId}/use")
    @Operation(summary = "标记回复已使用", description = "记录商家复制或使用某条差评回复")
    public Result<NegativeReplyVO> markNegativeReplyUsed(@Parameter(description = "回复 ID") @PathVariable Long replyId) {
        return Result.success(aiService.markNegativeReplyUsed(replyId));
    }

    @PutMapping("/{replyId}/effect")
    @Operation(summary = "更新回复效果", description = "标记回复后续效果，如纠纷平息、追加好评等")
    public Result<NegativeReplyVO> updateNegativeReplyEffect(@Parameter(description = "回复 ID") @PathVariable Long replyId,
                                                            @RequestBody NegativeReplyEffectDTO effectDTO) {
        return Result.success(aiService.updateNegativeReplyEffect(replyId, effectDTO));
    }

    @PutMapping("/{replyId}/favorite")
    @Operation(summary = "收藏或取消收藏回复", description = "维护商家常用差评回复模板")
    public Result<NegativeReplyVO> updateNegativeReplyFavorite(@Parameter(description = "回复 ID") @PathVariable Long replyId,
                                                              @RequestBody NegativeReplyFavoriteDTO favoriteDTO) {
        return Result.success(aiService.updateNegativeReplyFavorite(replyId, favoriteDTO));
    }
}
