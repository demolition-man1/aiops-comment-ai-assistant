package com.aiops.service.impl;

import com.aiops.dto.ProductQueryDTO;
import com.aiops.entity.BizProduct;
import com.aiops.mapper.BizProductMapper;
import com.aiops.mapper.BizSellerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

    @Mock
    private BizProductMapper productMapper;

    @Mock
    private BizSellerMapper sellerMapper;

    private CatalogServiceImpl catalogService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BizProduct.class);
        catalogService = new CatalogServiceImpl(productMapper, sellerMapper);
    }

    @Test
    void pageProductsKeywordSearchesCategoryAndProductId() {
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>());
        ProductQueryDTO queryDTO = new ProductQueryDTO();
        queryDTO.setKeyword("99a4788cb24856965c36a24e339b6058");

        catalogService.pageProducts(queryDTO);

        ArgumentCaptor<LambdaQueryWrapper<BizProduct>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();

        assertThat(sqlSegment).contains("categoryNameEn");
        assertThat(sqlSegment).contains("productId");
    }
}
