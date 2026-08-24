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
}
