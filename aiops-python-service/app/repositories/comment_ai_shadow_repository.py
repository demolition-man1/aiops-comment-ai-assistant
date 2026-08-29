from typing import Any


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
