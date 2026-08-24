package com.aiops.service.impl;

import com.aiops.dto.ProductQueryDTO;
import com.aiops.dto.SellerQueryDTO;
import com.aiops.entity.BizProduct;
import com.aiops.entity.BizSeller;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizProductMapper;
import com.aiops.mapper.BizSellerMapper;
import com.aiops.result.PageResult;
import com.aiops.service.CatalogService;
import com.aiops.vo.ProductVO;
import com.aiops.vo.SellerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final BizProductMapper productMapper;
    private final BizSellerMapper sellerMapper;

    @Override
    public PageResult<ProductVO> pageProducts(ProductQueryDTO queryDTO) {
        String categoryNameEn = blankToNull(queryDTO.getCategoryNameEn());
        String keyword = blankToNull(queryDTO.getKeyword());
        LambdaQueryWrapper<BizProduct> wrapper = new LambdaQueryWrapper<BizProduct>()
                .eq(categoryNameEn != null, BizProduct::getCategoryNameEn, categoryNameEn)
                .ge(queryDTO.getMinScore() != null, BizProduct::getAvgScore,
                        queryDTO.getMinScore() == null ? null : BigDecimal.valueOf(queryDTO.getMinScore()))
                .le(queryDTO.getMaxScore() != null, BizProduct::getAvgScore,
                        queryDTO.getMaxScore() == null ? null : BigDecimal.valueOf(queryDTO.getMaxScore()))
                .and(keyword != null, condition -> condition
                        .like(BizProduct::getCategoryNameEn, keyword)
                        .or()
                        .like(BizProduct::getProductId, keyword))
                .orderByDesc(BizProduct::getReviewCount);
        Page<BizProduct> page = productMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<ProductVO> records = page.getRecords().stream().map(this::toProductVO).toList();
        return PageResult.of(records, page.getTotal(), queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    public ProductVO getProduct(String productId) {
        BizProduct product = productMapper.selectOne(new LambdaQueryWrapper<BizProduct>()
                .eq(BizProduct::getProductId, productId));
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return toProductVO(product);
    }

    @Override
    public PageResult<SellerVO> pageSellers(SellerQueryDTO queryDTO) {
        LambdaQueryWrapper<BizSeller> wrapper = new LambdaQueryWrapper<BizSeller>()
                .eq(queryDTO.getState() != null, BizSeller::getSellerState, queryDTO.getState())
                .ge(queryDTO.getMinScore() != null, BizSeller::getAvgScore,
                        queryDTO.getMinScore() == null ? null : BigDecimal.valueOf(queryDTO.getMinScore()))
                .orderByDesc(BizSeller::getOrderCount);
        Page<BizSeller> page = sellerMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<SellerVO> records = page.getRecords().stream().map(this::toSellerVO).toList();
        return PageResult.of(records, page.getTotal(), queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    public SellerVO getSellerOverview(String sellerId) {
        BizSeller seller = sellerMapper.selectOne(new LambdaQueryWrapper<BizSeller>()
                .eq(BizSeller::getSellerId, sellerId));
        if (seller == null) {
            throw new BusinessException(404, "卖家不存在");
        }
        return toSellerVO(seller);
    }

    private ProductVO toProductVO(BizProduct product) {
        return new ProductVO(product.getProductId(), product.getSellerId(), product.getCategoryNameEn(),
                product.getAvgPrice(), product.getReviewCount(), product.getAvgScore(), product.getNegativeRate());
    }

    private SellerVO toSellerVO(BizSeller seller) {
        return new SellerVO(seller.getSellerId(), seller.getSellerState(), seller.getProductCount(),
                seller.getOrderCount(), seller.getAvgScore(), seller.getNegativeRate(), List.of());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
