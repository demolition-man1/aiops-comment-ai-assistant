package com.aiops.controller;

import com.aiops.dto.PromptTemplateDTO;
import com.aiops.dto.PromptTemplateQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.PromptTemplateVO;
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
@RequestMapping("/api/prompt-templates")
@RequiredArgsConstructor
@Tag(name = "Prompt 模板", description = "维护 AI 生成使用的业务 Prompt 模板")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    @Operation(summary = "分页查询 Prompt 模板", description = "按业务类型、语言、关键词和启用状态筛选 Prompt 模板")
    public Result<PageResult<PromptTemplateVO>> pageTemplates(@ModelAttribute PromptTemplateQueryDTO queryDTO) {
        return Result.success(promptTemplateService.pageTemplates(queryDTO));
    }

    @GetMapping("/active")
    @Operation(summary = "查询启用 Prompt 模板", description = "返回指定业务类型和语言下可用的 Prompt 模板")
    public Result<List<PromptTemplateVO>> activeTemplates(
            @Parameter(description = "业务类型") @RequestParam(required = false) String businessType,
            @Parameter(description = "语言") @RequestParam(required = false) String language) {
        return Result.success(promptTemplateService.activeTemplates(businessType, language));
    }

    @PostMapping
    @Operation(summary = "创建 Prompt 模板", description = "新增一个 AI Prompt 模板")
    public Result<PromptTemplateVO> createTemplate(@RequestBody PromptTemplateDTO templateDTO) {
        return Result.success(promptTemplateService.createTemplate(templateDTO));
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "修改 Prompt 模板", description = "修改 Prompt 模板名称、业务类型、语言、内容和状态")
    public Result<PromptTemplateVO> updateTemplate(@Parameter(description = "模板 ID") @PathVariable Long templateId,
                                                   @RequestBody PromptTemplateDTO templateDTO) {
        return Result.success(promptTemplateService.updateTemplate(templateId, templateDTO));
    }

    @PutMapping("/{templateId}/status")
    @Operation(summary = "修改 Prompt 模板状态", description = "启用或停用一个 Prompt 模板")
    public Result<PromptTemplateVO> updateStatus(@Parameter(description = "模板 ID") @PathVariable Long templateId,
                                                 @Parameter(description = "启用状态，1 启用 0 停用")
                                                 @RequestParam Integer enabled) {
        return Result.success(promptTemplateService.updateStatus(templateId, enabled));
    }

    @PostMapping("/{templateId}/default")
    @Operation(summary = "设为默认模板", description = "将模板设为同业务类型和语言下的默认 Prompt")
    public Result<PromptTemplateVO> setDefault(@Parameter(description = "模板 ID") @PathVariable Long templateId) {
        return Result.success(promptTemplateService.setDefault(templateId));
    }
}
