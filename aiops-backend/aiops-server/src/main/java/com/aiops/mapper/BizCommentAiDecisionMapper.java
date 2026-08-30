package com.aiops.mapper;

import com.aiops.entity.BizCommentAiDecision;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BizCommentAiDecisionMapper extends BaseMapper<BizCommentAiDecision> {

    @Select("""
            select r.id as shadowResultId,
                   r.comment_id as commentId,
                   r.ai_primary_problem as acceptedProblemType,
                   p.confidence as confidence
            from biz_comment_ai_shadow_result r
            join biz_comment c on c.id = r.comment_id
            join json_table(
                r.ai_problems,
                '$[*]' columns (
                    problem_type varchar(64) path '$.type',
                    confidence decimal(6,4) path '$.confidence'
                )
            ) p on p.problem_type = r.ai_primary_problem
            where r.run_id = #{runId}
              and c.is_negative = 1
              and length(trim(coalesce(c.clean_content, c.review_content, ''))) > 0
              and coalesce(nullif(trim(c.manual_problem_type), ''), '') = ''
              and (c.problem_type is null or trim(c.problem_type) = '' or c.problem_type = 'other')
              and r.call_status = 'success'
              and r.json_valid = 1
              and r.evidence_valid = 1
              and nullif(trim(r.ai_primary_problem), '') is not null
              and p.confidence >= #{minConfidence}
            order by r.sample_order asc
            """)
    List<Map<String, Object>> selectEligibleCandidates(@Param("runId") Long runId,
                                                        @Param("minConfidence") BigDecimal minConfidence);

    @Select("""
            select count(1)
            from biz_comment_ai_decision d
            join biz_comment_ai_shadow_result r on r.id = d.shadow_result_id
            where r.run_id = #{runId} and d.active = 1
            """)
    Integer selectActiveCountByRunId(@Param("runId") Long runId);

    @Update("update biz_comment_ai_decision set active = 0 where comment_id = #{commentId} and active = 1")
    int deactivateByCommentId(@Param("commentId") Long commentId);
}
