package com.aiops.mapper;

import com.aiops.entity.BizProduct;
import com.aiops.vo.DistributionItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BizProductMapper extends BaseMapper<BizProduct> {

    @Select("""
            <script>
            select coalesce(category_name_en, category_name, 'unknown') as name, count(1) as count
            from biz_product
            <where>
                <if test="sellerId != null and sellerId != ''">seller_id = #{sellerId}</if>
                <if test="productId != null and productId != ''">product_id = #{productId}</if>
            </where>
            group by coalesce(category_name_en, category_name, 'unknown')
            order by count desc
            limit 10
            </script>
            """)
    List<DistributionItemVO> selectCategoryDistribution(@Param("productId") String productId,
                                                        @Param("sellerId") String sellerId);

    @Select("""
            select product_id as product_id,
                   seller_id as seller_id,
                   category_name_en as category_name_en,
                   avg_price as avg_price,
                   review_count as review_count,
                   avg_score as avg_score,
                   negative_rate as negative_rate
            from biz_product
            where review_count is not null and review_count > 0
            order by review_count desc
            limit #{limit}
            """)
    List<com.aiops.vo.ProductVO> selectHotProducts(@Param("limit") Integer limit);

    @Select("""
            select product_id as product_id,
                   seller_id as seller_id,
                   category_name_en as category_name_en,
                   avg_price as avg_price,
                   review_count as review_count,
                   avg_score as avg_score,
                   negative_rate as negative_rate
            from biz_product
            where review_count is not null and review_count >= #{minReviewCount}
            order by negative_rate desc, review_count desc
            limit #{limit}
            """)
    List<com.aiops.vo.ProductVO> selectHighRiskProducts(@Param("minReviewCount") Integer minReviewCount,
                                                        @Param("limit") Integer limit);

    @Select("""
            select product_id as product_id,
                   seller_id as seller_id,
                   category_name_en as category_name_en,
                   avg_price as avg_price,
                   review_count as review_count,
                   avg_score as avg_score,
                   negative_rate as negative_rate
            from biz_product
            where review_count is not null and review_count >= #{minReviewCount}
            order by avg_score desc, review_count desc
            limit #{limit}
            """)
    List<com.aiops.vo.ProductVO> selectTopRatedProducts(@Param("minReviewCount") Integer minReviewCount,
                                                        @Param("limit") Integer limit);
}
