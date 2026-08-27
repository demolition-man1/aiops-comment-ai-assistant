package com.aiops.mapper;

import com.aiops.entity.BizProduct;
import com.aiops.vo.CategoryAnalysisVO;
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
            with category_base as (
                select coalesce(nullif(p.category_name_en, ''), nullif(p.category_name, ''), 'unknown') as category_name,
                       count(distinct p.product_id) as product_count,
                       count(c.id) as comment_count,
                       coalesce(round(avg(c.review_score), 2), 0) as avg_score,
                       coalesce(sum(case when c.is_negative = 1 then 1 else 0 end), 0) as negative_count
                from biz_product p
                left join biz_comment c on c.product_id = p.product_id
                group by coalesce(nullif(p.category_name_en, ''), nullif(p.category_name, ''), 'unknown')
            ),
            problem_rank as (
                select problem_source.category_name,
                       problem_source.problem_type,
                       problem_source.problem_count,
                       row_number() over (
                           partition by problem_source.category_name
                           order by problem_source.problem_count desc, problem_source.problem_type
                       ) as rank_no
                from (
                    select coalesce(nullif(p.category_name_en, ''), nullif(p.category_name, ''), 'unknown') as category_name,
                           coalesce(nullif(c.manual_problem_type, ''), nullif(c.problem_type, ''), 'unclassified') as problem_type,
                           count(1) as problem_count
                    from biz_product p
                    inner join biz_comment c on c.product_id = p.product_id
                    where c.is_negative = 1
                    group by coalesce(nullif(p.category_name_en, ''), nullif(p.category_name, ''), 'unknown'),
                             coalesce(nullif(c.manual_problem_type, ''), nullif(c.problem_type, ''), 'unclassified')
                ) problem_source
            )
            select category_base.category_name as category_name,
                   category_base.product_count as product_count,
                   category_base.comment_count as comment_count,
                   category_base.avg_score as avg_score,
                   category_base.negative_count as negative_count,
                   round(case
                       when category_base.comment_count = 0 then 0
                       else category_base.negative_count * 1.0 / category_base.comment_count
                   end, 4) as negative_rate,
                   coalesce(problem_rank.problem_type, 'unclassified') as top_problem_type,
                   coalesce(problem_rank.problem_count, 0) as top_problem_count,
                   case
                       when category_base.comment_count = 0 then 'none'
                       when category_base.negative_count * 1.0 / category_base.comment_count >= 0.2 then 'high'
                       when category_base.negative_count * 1.0 / category_base.comment_count >= 0.1 then 'medium'
                       else 'low'
                   end as risk_level
            from category_base
            left join problem_rank
                on problem_rank.category_name = category_base.category_name
               and problem_rank.rank_no = 1
            order by negative_rate desc, category_base.comment_count desc
            limit #{limit}
            """)
    List<CategoryAnalysisVO> selectCategoryAnalysis(@Param("limit") Integer limit);

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
