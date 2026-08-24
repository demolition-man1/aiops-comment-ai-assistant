package com.aiops.mapper;

import com.aiops.entity.BizComment;
import com.aiops.vo.DistributionItemVO;
import com.aiops.vo.KeywordItemVO;
import com.aiops.vo.TrendItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

public interface BizCommentMapper extends BaseMapper<BizComment> {

    @Select("""
            select coalesce(round(avg(review_score), 2), 0)
            from biz_comment
            where review_score is not null
            """)
    BigDecimal selectAverageReviewScore();

    @Select("""
            <script>
            select coalesce(cast(review_score as char), 'unrated') as name, count(1) as count
            from biz_comment
            <where>
                <if test="targetType == 'product'">product_id = #{targetId}</if>
                <if test="targetType == 'seller'">seller_id = #{targetId}</if>
            </where>
            group by review_score
            order by review_score
            </script>
            """)
    List<DistributionItemVO> selectScoreDistribution(@Param("targetType") String targetType,
                                                     @Param("targetId") String targetId);

    @Select("""
            <script>
            select coalesce(sentiment, 'unknown') as name, count(1) as count
            from biz_comment
            <where>
                <if test="targetType == 'product'">product_id = #{targetId}</if>
                <if test="targetType == 'seller'">seller_id = #{targetId}</if>
            </where>
            group by sentiment
            order by count desc
            </script>
            """)
    List<DistributionItemVO> selectSentimentDistribution(@Param("targetType") String targetType,
                                                         @Param("targetId") String targetId);

    @Select("""
            <script>
            select coalesce(problem_type, 'unknown') as name, count(1) as count
            from biz_comment
            <where>
                <if test="targetType == 'product'">product_id = #{targetId}</if>
                <if test="targetType == 'seller'">seller_id = #{targetId}</if>
                and is_negative = 1
                and problem_type is not null
            </where>
            group by problem_type
            order by count desc
            limit 10
            </script>
            """)
    List<DistributionItemVO> selectProblemDistribution(@Param("targetType") String targetType,
                                                       @Param("targetId") String targetId);

    @Select("""
            <script>
            select date_format(review_time, '%Y-%m') as time_bucket,
                   count(1) as comment_count,
                   sum(case when is_negative = 1 then 1 else 0 end) as negative_count,
                   round(sum(case when is_negative = 1 then 1 else 0 end) * 100.0 / count(1), 2) as negative_rate,
                   round(avg(review_score), 2) as avg_score
            from biz_comment
            <where>
                <if test="targetType == 'product'">product_id = #{targetId}</if>
                <if test="targetType == 'seller'">seller_id = #{targetId}</if>
                and review_time is not null
            </where>
            group by date_format(review_time, '%Y-%m')
            order by time_bucket
            </script>
            """)
    List<TrendItemVO> selectTrendDistribution(@Param("targetType") String targetType,
                                              @Param("targetId") String targetId);

    @Select("""
            <script>
            select coalesce(nullif(manual_problem_type, ''), nullif(problem_type, ''), '未分类') as keyword,
                   count(1) as count
            from biz_comment
            <where>
                <if test="targetType == 'product'">product_id = #{targetId}</if>
                <if test="targetType == 'seller'">seller_id = #{targetId}</if>
                and is_negative = 1
            </where>
            group by coalesce(nullif(manual_problem_type, ''), nullif(problem_type, ''), '未分类')
            order by count desc
            limit 10
            </script>
            """)
    List<KeywordItemVO> selectNegativeKeywordFallback(@Param("targetType") String targetType,
                                                      @Param("targetId") String targetId);
}
