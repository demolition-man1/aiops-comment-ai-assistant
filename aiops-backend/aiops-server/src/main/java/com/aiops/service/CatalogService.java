package com.aiops.service;

import com.aiops.dto.ProductQueryDTO;
import com.aiops.dto.SellerQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.ProductVO;
import com.aiops.vo.SellerVO;

public interface CatalogService {
    PageResult<ProductVO> pageProducts(ProductQueryDTO queryDTO);

    ProductVO getProduct(String productId);

    PageResult<SellerVO> pageSellers(SellerQueryDTO queryDTO);

    SellerVO getSellerOverview(String sellerId);
}

