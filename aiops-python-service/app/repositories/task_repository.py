from typing import Any


def update_analysis_task(conn: Any, task_id: int, status: str, progress: int, error_message: str | None = None) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            update biz_analysis_task
            set task_status = %s,
                progress = %s,
                error_message = %s,
                end_time = case when %s in ('success', 'partial', 'budget_stopped', 'failed') then now() else end_time end,
                update_time = now()
            where id = %s
            """,
            (status, progress, error_message, status, task_id),
        )


def update_crawl_task(
    conn: Any,
    task_id: int,
    status: str,
    progress: int,
    success_count: int = 0,
    fail_count: int = 0,
    error_message: str | None = None,
) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            update biz_crawl_task
            set task_status = %s,
                progress = %s,
                success_count = %s,
                fail_count = %s,
                error_message = %s,
                end_time = case when %s in ('success', 'failed') then now() else end_time end,
                update_time = now()
            where id = %s
            """,
            (status, progress, success_count, fail_count, error_message, status, task_id),
        )
