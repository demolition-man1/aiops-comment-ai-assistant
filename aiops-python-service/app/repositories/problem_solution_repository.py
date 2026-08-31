from typing import Any


def list_enabled_problem_solutions(conn: Any) -> list[dict[str, Any]]:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            select id, problem_type, category_name_en, solution_title, solution_content,
                   keywords, source_type, priority, update_time
            from biz_problem_solution
            where enabled = 1
            order by priority desc, id asc
            """
        )
        return list(cursor.fetchall())
