from contextlib import contextmanager

from app.ai.errors import AiAuthenticationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import CommentAnalysisOutput
from app.services.comment_ai_shadow_service import CommentAiShadowService


class FakeCursor:
    def __init__(self, candidates: list[dict[str, object]]) -> None:
        self.candidates = candidates
        self.calls: list[tuple[object, ...]] = []
        self.current_sql = ""
        self.reserved: list[dict[str, object]] = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None

    def execute(self, sql, params=None) -> None:
        self.current_sql = " ".join(sql.split())
        self.calls.append(("execute", self.current_sql, params))

    def executemany(self, sql, rows) -> None:
        self.current_sql = " ".join(sql.split())
        copied_rows = list(rows)
        self.calls.append(("executemany", self.current_sql, copied_rows))
        if "insert ignore into biz_comment_ai_shadow_result" in self.current_sql:
            self.reserved.extend(copied_rows)
        self.rowcount = len(copied_rows)

    def fetchall(self) -> list[dict[str, object]]:
        if "from biz_comment_ai_shadow_result" not in self.current_sql:
            return self.candidates
        by_id = {int(item["id"]): item for item in self.candidates}
        return [
            {
                **by_id[int(item["comment_id"])],
                "comment_id": item["comment_id"],
                "sample_order": item["sample_order"],
            }
            for item in self.reserved
        ]


class FakeConn:
    def __init__(self, candidates: list[dict[str, object]]) -> None:
        self.cursor_instance = FakeCursor(candidates)

    def cursor(self) -> FakeCursor:
        return self.cursor_instance

    def commit(self) -> None:
        return None

    def rollback(self) -> None:
        return None

    def close(self) -> None:
        return None


class FakeProvider:
    def __init__(self, result: AiInvocationResult[CommentAnalysisOutput] | None = None, error: Exception | None = None) -> None:
        self.result = result
        self.error = error
        self.calls = 0

    def invoke_structured(self, prompt, schema):
        self.calls += 1
        if self.error is not None:
            raise self.error
        assert self.result is not None
        return self.result

    def invoke_text(self, prompt, **kwargs):
        raise AssertionError("repair is not expected for a valid fake response")


@contextmanager
def connection_factory(conn: FakeConn):
    yield conn


def request(max_total_tokens: int = 6000, sample_size: int = 2) -> dict[str, object]:
    return {
        "taskId": 9,
        "runId": 17,
        "targetType": "product",
        "targetId": "product-17",
        "sampleSize": sample_size,
        "sampleSeed": 20260829,
        "maxTotalTokens": max_total_tokens,
        "promptTemplate": "Classify score {reviewScore}: {reviewText}",
        "promptVariables": {},
    }


def candidates() -> list[dict[str, object]]:
    return [
        {
            "id": 31,
            "review_score": 1,
            "review_content": "The parcel arrived late.",
            "clean_content": "The parcel arrived late.",
            "sentiment": "negative",
            "problem_type": "logistics",
        },
        {
            "id": 32,
            "review_score": 3,
            "review_content": "It arrived late, but I can wait.",
            "clean_content": "It arrived late, but I can wait.",
            "sentiment": "neutral",
            "problem_type": None,
        },
    ]


def analysis_result(tokens: int = 1200) -> AiInvocationResult[CommentAnalysisOutput]:
    return AiInvocationResult(
        value=CommentAnalysisOutput.model_validate(
            {
                "sentiment": "negative",
                "sentimentConfidence": 0.91,
                "primaryProblem": "delivery",
                "problems": [
                    {"type": "delivery", "confidence": 0.91, "evidence": "arrived late"}
                ],
            }
        ),
        model_name="deepseek-chat",
        input_tokens=12,
        output_tokens=tokens - 12,
        total_tokens=tokens,
        token_usage_estimated=False,
    )


def test_shadow_service_persists_valid_comparisons_without_updating_comments() -> None:
    conn = FakeConn(candidates())
    provider = FakeProvider(result=analysis_result())
    service = CommentAiShadowService(provider=provider, connection_factory=lambda: connection_factory(conn))

    result = service.run(request())

    assert result == {
        "success": True,
        "runId": 17,
        "status": "success",
        "actualSampleSize": 2,
        "successCount": 2,
        "failureCount": 0,
        "totalCalls": 2,
        "totalTokens": 2400,
        "modelName": "deepseek-chat",
    }
    sql_calls = [call[1] for call in conn.cursor_instance.calls]
    assert any("update biz_comment_ai_shadow_result" in sql for sql in sql_calls)
    assert not any("update biz_comment set" in sql for sql in sql_calls)


def test_shadow_service_stops_before_next_call_when_token_budget_is_exhausted() -> None:
    conn = FakeConn(candidates())
    provider = FakeProvider(result=analysis_result(tokens=1200))
    service = CommentAiShadowService(provider=provider, connection_factory=lambda: connection_factory(conn))

    result = service.run(request(max_total_tokens=1000))

    assert result["status"] == "budget_stopped"
    assert result["successCount"] == 1
    assert result["totalCalls"] == 1
    assert provider.calls == 1


def test_shadow_service_aborts_after_the_first_authentication_failure() -> None:
    conn = FakeConn(candidates())
    provider = FakeProvider(error=AiAuthenticationError("AI provider authentication failed"))
    service = CommentAiShadowService(provider=provider, connection_factory=lambda: connection_factory(conn))

    result = service.run(request())

    assert result["success"] is False
    assert result["status"] == "failed"
    assert result["failureCount"] == 1
    assert result["totalCalls"] == 1
    assert provider.calls == 1
    assert result["message"] == "AI provider authentication failed"


def test_shadow_service_persists_results_in_batches_of_ten() -> None:
    batch_candidates = [
        {
            "id": index,
            "review_score": 1,
            "review_content": f"The parcel arrived late {index}.",
            "clean_content": f"The parcel arrived late {index}.",
            "sentiment": "negative",
            "problem_type": "logistics",
        }
        for index in range(1, 12)
    ]
    conn = FakeConn(batch_candidates)
    provider = FakeProvider(result=analysis_result())
    service = CommentAiShadowService(provider=provider, connection_factory=lambda: connection_factory(conn))

    result = service.run(request(max_total_tokens=100000, sample_size=11))

    update_batches = [
        call[2]
        for call in conn.cursor_instance.calls
        if call[0] == "executemany" and "update biz_comment_ai_shadow_result" in call[1]
    ]
    assert result["status"] == "success"
    assert [len(rows) for rows in update_batches] == [10, 1]
