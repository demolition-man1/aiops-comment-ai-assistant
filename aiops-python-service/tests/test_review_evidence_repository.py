import unittest

from app.repositories.review_evidence_repository import list_review_evidence


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


class ReviewEvidenceRepositoryTests(unittest.TestCase):
    def test_repository_selects_text_bearing_comments_with_labels_and_fixed_limit(self) -> None:
        conn = FakeConn([{"id": 36, "clean_content": "Delivery is delayed."}])

        rows = list_review_evidence(conn, limit=240)

        self.assertEqual(rows, [{"id": 36, "clean_content": "Delivery is delayed."}])
        _, sql, params = conn.cursor_instance.calls[0]
        self.assertIn("from biz_comment c", sql)
        self.assertIn("left join biz_comment_ai_decision d", sql)
        self.assertIn("coalesce(nullif(trim(c.clean_content), ''), nullif(trim(c.review_content), ''))", sql)
        self.assertIn("manual_problem_type", sql)
        self.assertIn("order by c.review_time desc, c.id desc", sql)
        self.assertIn("limit %s", sql)
        self.assertNotIn("select *", sql)
        self.assertEqual(params, (240,))

    def test_repository_returns_no_rows_without_query_when_limit_is_zero(self) -> None:
        conn = FakeConn([])

        rows = list_review_evidence(conn, limit=0)

        self.assertEqual(rows, [])
        self.assertEqual(conn.cursor_instance.calls, [])


if __name__ == "__main__":
    unittest.main()
