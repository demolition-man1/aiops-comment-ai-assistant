from typing import Any


def replace_comments(conn: Any, rows: list[dict[str, Any]]) -> int:
    if not rows:
        return 0
    review_ids = tuple(dict.fromkeys(row.get("review_id") for row in rows if row.get("review_id")))
    sql = """
        insert into biz_comment
        (review_id, order_id, product_id, seller_id, review_score, review_title, review_content,
         clean_content, review_time, sentiment, sentiment_score, keywords, problem_type, is_negative,
         create_time, update_time)
        values
        (%(review_id)s, %(order_id)s, %(product_id)s, %(seller_id)s, %(review_score)s, %(review_title)s,
         %(review_content)s, %(clean_content)s, %(review_time)s, %(sentiment)s, %(sentiment_score)s,
         %(keywords)s, %(problem_type)s, %(is_negative)s, now(), now())
    """
    with conn.cursor() as cursor:
        if review_ids:
            placeholders = ", ".join(["%s"] * len(review_ids))
            cursor.execute(f"delete from biz_comment where review_id in ({placeholders})", review_ids)
        cursor.executemany(sql, rows)
    return len(rows)


def fetch_comments(conn: Any, target_type: str, target_id: str) -> list[dict[str, Any]]:
    column = "product_id" if target_type == "product" else "seller_id"
    with conn.cursor() as cursor:
        cursor.execute(
            f"""
            select id, review_id, order_id, product_id, seller_id, review_score, review_title,
                   review_content, clean_content, review_time, sentiment, sentiment_score,
                   keywords, problem_type, manual_problem_type, custom_tags, is_negative
            from biz_comment
            where {column} = %s
            """,
            (target_id,),
        )
        return list(cursor.fetchall())


def fetch_comment_by_id(conn: Any, comment_id: int) -> dict[str, Any] | None:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            select id, product_id, seller_id, review_score, review_content, clean_content,
                   sentiment, problem_type
            from biz_comment
            where id = %s
            """,
            (comment_id,),
        )
        return cursor.fetchone()
