package com.aiops.controller;

import com.aiops.dto.SellerQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.CatalogService;
import com.aiops.vo.SellerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "商家目录", description = "商家列表和商家概览查询")
public class SellerController {

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "分页查询商家", description = "按商家 ID、城市、州等条件查询已导入商家")
    public Result<PageResult<SellerVO>> pageSellers(@ModelAttribute SellerQueryDTO queryDTO) {
        return Result.success(catalogService.pageSellers(queryDTO));
    }

    @GetMapping("/{sellerId}/overview")
    @Operation(summary = "商家概览", description = "查询指定商家的基础信息和评论聚合概览")
    public Result<SellerVO> getSellerOverview(@Parameter(description = "商家 ID") @PathVariable String sellerId) {
        return Result.success(catalogService.getSellerOverview(sellerId));
    }
}
