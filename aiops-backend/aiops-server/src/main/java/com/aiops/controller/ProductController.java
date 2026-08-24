package com.aiops.controller;

import com.aiops.dto.ProductQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.CatalogService;
import com.aiops.vo.ProductVO;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "商品目录", description = "商品列表和商品详情查询")
public class ProductController {

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "分页查询商品", description = "按商品 ID、类目、商家等条件查询已导入商品")
    public Result<PageResult<ProductVO>> pageProducts(@ModelAttribute ProductQueryDTO queryDTO) {
        return Result.success(catalogService.pageProducts(queryDTO));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "商品详情", description = "根据 Olist 商品 ID 查询商品信息")
    public Result<ProductVO> getProduct(@Parameter(description = "商品 ID") @PathVariable String productId) {
        return Result.success(catalogService.getProduct(productId));
    }
}
