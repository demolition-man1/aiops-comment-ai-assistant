import unittest

from app.repositories.negative_reply_repository import list_eligible_historical_replies
from app.repositories.problem_solution_repository import list_enabled_problem_solutions


class FakeCursor:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self.rows = rows
        self.calls: list[tuple[str, str, object]] = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None

    def execute(self, sql: str, params=None) -> None:
        self.calls.append(("execute", " ".join(sql.split()), params))

    def fetchall(self) -> list[dict[str, object]]:
        return self.rows


class FakeConn:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self.cursor_instance = FakeCursor(rows)

    def cursor(self) -> FakeCursor:
        return self.cursor_instance


class RagRepositoryTests(unittest.TestCase):
    def test_problem_solution_repository_selects_only_enabled_rows_with_explicit_columns(self) -> None:
        conn = FakeConn([{"id": 14, "solution_title": "Follow shipping progress"}])

        rows = list_enabled_problem_solutions(conn)

        self.assertEqual(rows, [{"id": 14, "solution_title": "Follow shipping progress"}])
        _, sql, params = conn.cursor_instance.calls[0]
        self.assertIn("from biz_problem_solution", sql)
        self.assertIn("where enabled = 1", sql)
        self.assertIn("order by priority desc, id asc", sql)
        self.assertNotIn("select *", sql)
        self.assertIsNone(params)

    def test_negative_reply_repository_selects_only_approved_knowledge_rows(self) -> None:
        conn = FakeConn([{"id": 25, "effect_tag": "resolved", "favorite_flag": 0}])

        rows = list_eligible_historical_replies(conn)

        self.assertEqual(rows, [{"id": 25, "effect_tag": "resolved", "favorite_flag": 0}])
        _, sql, params = conn.cursor_instance.calls[0]
        self.assertIn("from biz_negative_reply", sql)
        self.assertIn("favorite_flag = 1", sql)
        self.assertIn("effect_tag in ('resolved', 'positive_followup')", sql)
        self.assertIn("reply_content is not null", sql)
        self.assertNotIn("select *", sql)
        self.assertIsNone(params)


if __name__ == "__main__":
    unittest.main()
