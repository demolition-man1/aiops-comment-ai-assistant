from app.repositories.task_repository import update_analysis_task


class FakeCursor:
    def __init__(self) -> None:
        self.call: tuple[str, tuple[object, ...]] | None = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None

    def execute(self, sql: str, params: tuple[object, ...]) -> None:
        self.call = (" ".join(sql.split()), params)


class FakeConn:
    def __init__(self) -> None:
        self.cursor_instance = FakeCursor()

    def cursor(self) -> FakeCursor:
        return self.cursor_instance


def test_partial_analysis_task_is_treated_as_a_terminal_status() -> None:
    conn = FakeConn()

    update_analysis_task(conn, task_id=9, status="partial", progress=100)

    assert conn.cursor_instance.call is not None
    sql, params = conn.cursor_instance.call
    assert "'success', 'partial', 'budget_stopped', 'failed'" in sql
    assert params == ("partial", 100, None, "partial", 9)
