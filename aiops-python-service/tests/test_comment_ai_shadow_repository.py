import unittest

from app.repositories.comment_ai_shadow_repository import reserve_shadow_results
from app.repositories.comment_repository import fetch_shadow_candidates


class FakeCursor:
    def __init__(self, rowcount: int = 0) -> None:
        self.rowcount = rowcount
        self.calls: list[tuple[object, ...]] = []
        self.rows: list[dict[str, object]] = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None

    def execute(self, sql, params=None) -> None:
        self.calls.append(("execute", " ".join(sql.split()), params))

    def executemany(self, sql, rows) -> None:
        self.calls.append(("executemany", " ".join(sql.split()), rows))

    def fetchall(self) -> list[dict[str, object]]:
        return self.rows


class FakeConn:
    def __init__(self, rowcount: int = 0) -> None:
        self.cursor_instance = FakeCursor(rowcount)

    def cursor(self) -> FakeCursor:
        return self.cursor_instance


class CommentAiShadowRepositoryTests(unittest.TestCase):
    def test_reserve_shadow_results_keeps_rule_baseline_and_uses_duplicate_safe_insert(self) -> None:
        conn = FakeConn(rowcount=2)

        reserved = reserve_shadow_results(
            conn,
            run_id=17,
            comments=[
                {"id": 31, "sentiment": "negative", "problem_type": "logistics"},
                {"id": 32, "sentiment": "neutral", "problem_type": None},
            ],
        )

        self.assertEqual(reserved, 2)
        operation, sql, rows = conn.cursor_instance.calls[0]
        self.assertEqual(operation, "executemany")
        self.assertIn("insert ignore into biz_comment_ai_shadow_result", sql)
        self.assertEqual(
            rows,
            [
                {
                    "run_id": 17,
                    "comment_id": 31,
                    "sample_order": 1,
                    "rule_sentiment": "negative",
                    "rule_problem_type": "logistics",
                    "call_status": "pending",
                },
                {
                    "run_id": 17,
                    "comment_id": 32,
                    "sample_order": 2,
                    "rule_sentiment": "neutral",
                    "rule_problem_type": None,
                    "call_status": "pending",
                },
            ],
        )

    def test_fetch_shadow_candidates_uses_only_the_requested_target_dimension(self) -> None:
        conn = FakeConn()
        conn.cursor_instance.rows = [{"id": 31, "review_content": "late delivery"}]

        candidates = fetch_shadow_candidates(conn, "product", "product-17")

        self.assertEqual(candidates, [{"id": 31, "review_content": "late delivery"}])
        operation, sql, params = conn.cursor_instance.calls[0]
        self.assertEqual(operation, "execute")
        self.assertIn("where product_id = %s", sql)
        self.assertEqual(params, ("product-17",))

    def test_fetch_shadow_candidates_rejects_unknown_target_type_before_querying(self) -> None:
        conn = FakeConn()

        with self.assertRaisesRegex(ValueError, "target_type"):
            fetch_shadow_candidates(conn, "category", "office")

        self.assertEqual(conn.cursor_instance.calls, [])


if __name__ == "__main__":
    unittest.main()
