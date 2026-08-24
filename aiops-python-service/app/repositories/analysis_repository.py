from typing import Any


def insert_analysis_result(conn: Any, row: dict[str, Any]) -> int:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            insert into biz_comment_analysis_result
            (task_id, target_type, target_id, total_count, positive_count, neutral_count,
             negative_count, positive_rate, negative_rate, top_keywords, negative_keywords,
             problem_distribution, score_distribution, custom_tag_distribution,
             trend_distribution, summary, create_time)
            values
            (%(task_id)s, %(target_type)s, %(target_id)s, %(total_count)s, %(positive_count)s,
             %(neutral_count)s, %(negative_count)s, %(positive_rate)s, %(negative_rate)s,
             %(top_keywords)s, %(negative_keywords)s, %(problem_distribution)s,
             %(score_distribution)s, %(custom_tag_distribution)s, %(trend_distribution)s,
             %(summary)s, now())
            """,
            row,
        )
        return int(cursor.lastrowid)


def fetch_latest_analysis(conn: Any, target_type: str, target_id: str) -> dict[str, Any] | None:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            select *
            from biz_comment_analysis_result
            where target_type = %s and target_id = %s
            order by create_time desc
            limit 1
            """,
            (target_type, target_id),
        )
        return cursor.fetchone()
