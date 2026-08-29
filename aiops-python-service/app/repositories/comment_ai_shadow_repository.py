from typing import Any

import json


def reserve_shadow_results(conn: Any, run_id: int, comments: list[dict[str, Any]]) -> int:
    if not comments:
        return 0
    rows = [
        {
            "run_id": run_id,
            "comment_id": int(comment["id"]),
            "sample_order": index,
            "rule_sentiment": comment.get("sentiment"),
            "rule_problem_type": comment.get("problem_type"),
            "call_status": "pending",
        }
        for index, comment in enumerate(comments, start=1)
    ]
    with conn.cursor() as cursor:
        cursor.executemany(
            """
            insert ignore into biz_comment_ai_shadow_result
            (run_id, comment_id, sample_order, rule_sentiment, rule_problem_type, call_status, create_time)
            values
            (%(run_id)s, %(comment_id)s, %(sample_order)s, %(rule_sentiment)s, %(rule_problem_type)s,
             %(call_status)s, now())
            """,
            rows,
        )
        return int(cursor.rowcount)


def start_shadow_run(conn: Any, run_id: int, actual_sample_size: int) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            update biz_comment_ai_shadow_run
            set actual_sample_size = %s,
                run_status = 'processing',
                start_time = coalesce(start_time, now()),
                update_time = now()
            where id = %s
            """,
            (actual_sample_size, run_id),
        )


def fetch_pending_shadow_results(conn: Any, run_id: int) -> list[dict[str, Any]]:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            select result.comment_id, result.sample_order, result.rule_sentiment, result.rule_problem_type,
                   comment.review_score, comment.review_content, comment.clean_content
            from biz_comment_ai_shadow_result result
            join biz_comment comment on comment.id = result.comment_id
            where result.run_id = %s and result.call_status = 'pending'
            order by result.sample_order asc
            """,
            (run_id,),
        )
        return list(cursor.fetchall())


def success_shadow_result_update(
    run_id: int,
    comment_id: int,
    output: Any,
    model_name: str,
    token_usage: int,
    token_usage_estimated: bool,
    latency_ms: int,
) -> dict[str, Any]:
    problems = [problem.model_dump() for problem in output.problems]
    evidence = [problem.evidence for problem in output.problems]
    return {
        "run_id": run_id,
        "comment_id": comment_id,
        "ai_sentiment": output.sentiment,
        "ai_sentiment_confidence": output.sentiment_confidence,
        "ai_primary_problem": output.primary_problem,
        "ai_problems": json.dumps(problems, ensure_ascii=False),
        "ai_evidence": json.dumps(evidence, ensure_ascii=False),
        "json_valid": 1,
        "evidence_valid": 1,
        "call_status": "success",
        "model_name": model_name,
        "token_usage": token_usage,
        "token_usage_estimated": 1 if token_usage_estimated else 0,
        "latency_ms": latency_ms,
        "error_message": None,
    }


def failed_shadow_result_update(
    run_id: int,
    comment_id: int,
    error_message: str,
    token_usage: int,
    token_usage_estimated: bool,
    latency_ms: int,
) -> dict[str, Any]:
    return {
        "run_id": run_id,
        "comment_id": comment_id,
        "ai_sentiment": None,
        "ai_sentiment_confidence": None,
        "ai_primary_problem": None,
        "ai_problems": None,
        "ai_evidence": None,
        "json_valid": 0,
        "evidence_valid": 0,
        "call_status": "failed",
        "model_name": None,
        "token_usage": token_usage,
        "token_usage_estimated": 1 if token_usage_estimated else 0,
        "latency_ms": latency_ms,
        "error_message": error_message[:1000],
    }


def persist_shadow_result_batch(conn: Any, updates: list[dict[str, Any]]) -> None:
    if not updates:
        return
    with conn.cursor() as cursor:
        cursor.executemany(
            """
            update biz_comment_ai_shadow_result
            set ai_sentiment = %(ai_sentiment)s,
                ai_sentiment_confidence = %(ai_sentiment_confidence)s,
                ai_primary_problem = %(ai_primary_problem)s,
                ai_problems = %(ai_problems)s,
                ai_evidence = %(ai_evidence)s,
                json_valid = %(json_valid)s,
                evidence_valid = %(evidence_valid)s,
                call_status = %(call_status)s,
                model_name = %(model_name)s,
                token_usage = %(token_usage)s,
                token_usage_estimated = %(token_usage_estimated)s,
                latency_ms = %(latency_ms)s,
                error_message = %(error_message)s
            where run_id = %(run_id)s and comment_id = %(comment_id)s
            """,
            updates,
        )


def finish_shadow_run(
    conn: Any,
    run_id: int,
    status: str,
    total_calls: int,
    success_count: int,
    failure_count: int,
    total_tokens: int,
    latency_ms: int,
    error_message: str | None = None,
) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            update biz_comment_ai_shadow_run
            set run_status = %s,
                total_calls = %s,
                success_count = %s,
                failure_count = %s,
                total_tokens = %s,
                latency_ms = %s,
                error_message = %s,
                end_time = now(),
                update_time = now()
            where id = %s
            """,
            (
                status,
                total_calls,
                success_count,
                failure_count,
                total_tokens,
                latency_ms,
                error_message[:1000] if error_message else None,
                run_id,
            ),
        )
