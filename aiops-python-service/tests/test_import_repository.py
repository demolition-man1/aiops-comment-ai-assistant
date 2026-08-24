import unittest

from app.repositories.comment_repository import replace_comments


class FakeCursor:
    def __init__(self) -> None:
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None

    def execute(self, sql, params=None) -> None:
        self.calls.append(("execute", " ".join(sql.split()), params))

    def executemany(self, sql, rows) -> None:
        self.calls.append(("executemany", " ".join(sql.split()), rows))


class FakeConn:
    def __init__(self) -> None:
        self.cursor_instance = FakeCursor()

    def cursor(self) -> FakeCursor:
        return self.cursor_instance


class ImportRepositoryTests(unittest.TestCase):
    def test_replace_comments_removes_existing_review_ids_before_insert(self) -> None:
        conn = FakeConn()
        rows = [
            {"review_id": "review-a"},
            {"review_id": "review-b"},
            {"review_id": "review-a"},
            {"review_id": None},
        ]

        count = replace_comments(conn, rows)

        self.assertEqual(count, 4)
        delete_call = conn.cursor_instance.calls[0]
        self.assertEqual(delete_call[0], "execute")
        self.assertIn("delete from biz_comment where review_id in", delete_call[1])
        self.assertEqual(delete_call[2], ("review-a", "review-b"))
        self.assertEqual(conn.cursor_instance.calls[1][0], "executemany")


if __name__ == "__main__":
    unittest.main()
