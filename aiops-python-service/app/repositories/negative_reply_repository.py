from typing import Any


def list_eligible_historical_replies(conn: Any) -> list[dict[str, Any]]:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            select id, problem_type, comment_content, reply_content, effect_tag,
                   favorite_flag, use_count, update_time
            from biz_negative_reply
            where reply_content is not null
              and trim(reply_content) <> ''
              and (
                    favorite_flag = 1
                    or effect_tag in ('resolved', 'positive_followup')
                    or coalesce(use_count, 0) >= 3
                  )
            order by update_time desc, id asc
            """
        )
        return list(cursor.fetchall())
