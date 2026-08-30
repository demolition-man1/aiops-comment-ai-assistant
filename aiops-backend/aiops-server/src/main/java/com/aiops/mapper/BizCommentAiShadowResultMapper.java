package com.aiops.mapper;

import com.aiops.entity.BizCommentAiShadowResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface BizCommentAiShadowResultMapper extends BaseMapper<BizCommentAiShadowResult> {

    @Select("""
            select r.id as resultId,
                   r.run_id as runId,
                   r.comment_id as commentId,
                   r.sample_order as sampleOrder,
                   c.review_score as reviewScore,
                   coalesce(c.clean_content, c.review_content, '') as reviewContent,
                   r.rule_sentiment as ruleSentiment,
                   r.rule_problem_type as ruleProblemType,
                   r.ai_sentiment as aiSentiment,
                   r.ai_sentiment_confidence as aiSentimentConfidence,
                   r.ai_primary_problem as aiPrimaryProblem,
                   r.ai_problems as aiProblems,
                   r.ai_evidence as aiEvidence,
                   r.json_valid as jsonValid,
                   r.evidence_valid as evidenceValid,
                   r.call_status as callStatus,
                   r.model_name as modelName,
                   r.token_usage as tokenUsage,
                   r.token_usage_estimated as tokenUsageEstimated,
                   r.latency_ms as latencyMs,
                   r.error_message as errorMessage,
                   a.manual_sentiment as manualSentiment,
                   a.manual_problem_types as manualProblemTypes,
                   a.annotation_note as annotationNote,
                   a.annotation_time as annotationTime
            from biz_comment_ai_shadow_result r
            join biz_comment c on c.id = r.comment_id
            left join biz_comment_ai_annotation a on a.comment_id = r.comment_id
            where r.run_id = #{runId}
            order by r.sample_order asc
            """)
    List<Map<String, Object>> selectEvaluationRows(@Param("runId") Long runId);
}
