package com.aiops.controller;

import com.aiops.dto.CustomTagDTO;
import com.aiops.dto.CustomTagQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.TagLibraryService;
import com.aiops.vo.CustomTagVO;
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
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "自定义标签库", description = "维护商家业务标签，并用于评论人工标注")
public class TagLibraryController {

    private final TagLibraryService tagLibraryService;

    @GetMapping
    @Operation(summary = "分页查询标签", description = "按关键词、分组、启用状态筛选商家自定义标签")
    public Result<PageResult<CustomTagVO>> pageTags(@ModelAttribute CustomTagQueryDTO queryDTO) {
        return Result.success(tagLibraryService.pageTags(queryDTO));
    }

    @GetMapping("/active")
    @Operation(summary = "查询启用标签", description = "返回评论标注弹窗可直接选择的标签列表")
    public Result<List<CustomTagVO>> activeTags() {
        return Result.success(tagLibraryService.activeTags());
    }

    @PostMapping
    @Operation(summary = "创建标签", description = "新增一个商家自定义标签")
    public Result<CustomTagVO> createTag(@RequestBody CustomTagDTO customTagDTO) {
        return Result.success(tagLibraryService.createTag(customTagDTO));
    }

    @PutMapping("/{tagId}")
    @Operation(summary = "修改标签", description = "修改标签名称、分组、颜色、说明和排序")
    public Result<CustomTagVO> updateTag(@Parameter(description = "标签 ID") @PathVariable Long tagId,
                                         @RequestBody CustomTagDTO customTagDTO) {
        return Result.success(tagLibraryService.updateTag(tagId, customTagDTO));
    }

    @PutMapping("/{tagId}/status")
    @Operation(summary = "修改标签状态", description = "启用或停用一个自定义标签")
    public Result<CustomTagVO> updateStatus(@Parameter(description = "标签 ID") @PathVariable Long tagId,
                                            @Parameter(description = "启用状态，1 启用 0 停用")
                                            @RequestParam Integer enabled) {
        return Result.success(tagLibraryService.updateStatus(tagId, enabled));
    }
}
