from typing import Any


def list_review_evidence(conn: Any, *, limit: int) -> list[dict[str, Any]]:
    if limit <= 0:
        return []

    with conn.cursor() as cursor:
        cursor.execute(
            """
            select c.id, c.product_id, c.seller_id, c.review_score, c.sentiment,
                   coalesce(nullif(trim(c.manual_problem_type), ''),
                            nullif(trim(d.accepted_problem_type), ''),
                            nullif(trim(c.problem_type), ''), 'unknown') as problem_type,
                   c.review_time,
                   coalesce(nullif(trim(c.clean_content), ''), nullif(trim(c.review_content), '')) as clean_content,
                   c.review_content, c.update_time
            from biz_comment c
            left join biz_comment_ai_decision d on d.comment_id = c.id and d.active = 1
            where coalesce(nullif(trim(c.clean_content), ''), nullif(trim(c.review_content), '')) is not null
              and lower(trim(coalesce(nullif(trim(c.clean_content), ''), nullif(trim(c.review_content), ''))))
                  not in ('null', 'none', 'nan', 'nannan')
            order by c.review_time desc, c.id desc
            limit %s
            """,
            (limit,),
        )
        return list(cursor.fetchall())
