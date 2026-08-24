package com.aiops.controller;

import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.AiService;
import com.aiops.vo.AiContentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/contents")
@RequiredArgsConstructor
@Tag(name = "AI 文案", description = "商品标题、促销话术和推广文案生成记录")
public class AiContentController {

    private final AiService aiService;

    @PostMapping
    @Operation(summary = "生成 AI 文案", description = "根据目标对象、文案类型、风格和语言生成运营文案")
    public Result<AiContentVO> generateContent(@RequestBody AiContentGenerateDTO generateDTO) {
        return Result.success(aiService.generateContent(generateDTO));
    }

    @GetMapping
    @Operation(summary = "分页查询 AI 文案", description = "查询历史生成的 AI 文案记录")
    public Result<PageResult<AiContentVO>> pageContents(
            @Parameter(description = "目标类型，product/seller") @RequestParam(required = false) String targetType,
            @Parameter(description = "目标 ID") @RequestParam(required = false) String targetId,
            @Parameter(description = "文案类型") @RequestParam(required = false) String contentType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(aiService.pageContents(targetType, targetId, contentType, pageNum, pageSize));
    }
}
