from app.rag.models import RagReference, RagRetrievalResult
from app.rag.report_rag_service import ReportRagService
from app.services.ai_service import AiService
from types import SimpleNamespace
from unittest.mock import patch


class FakeEvidenceRetriever:
    def __init__(self, result: RagRetrievalResult) -> None:
        self.result = result
        self.calls: list[dict[str, object]] = []

    def retrieve(self, **kwargs):
        self.calls.append(kwargs)
        return self.result


class FakeSolutionRetriever:
    def __init__(self, result: RagRetrievalResult) -> None:
        self.result = result
        self.calls: list[dict[str, object]] = []

    def retrieve(self, **kwargs):
        self.calls.append(kwargs)
        return self.result


def _request() -> dict[str, object]:
    return {
        "targetType": "product",
        "targetId": "product-a",
        "language": "en-US",
        "analysisResult": {
            "summary": "Delivery delays are the primary risk.",
            "negativeKeywords": '[{"name":"late delivery","value":8}]',
            "problemDistribution": '[{"name":"logistics","value":8},{"name":"quality","value":2}]',
        },
    }


def test_report_rag_retrieves_target_evidence_and_problem_matched_solution() -> None:
    evidence_reference = RagReference("review_evidence", 31, "Review evidence #31", 0.93)
    solution_reference = RagReference("problem_solution", 8, "Delivery checklist", 0.87)
    evidence = FakeEvidenceRetriever(RagRetrievalResult("Review evidence context", [evidence_reference]))
    solutions = FakeSolutionRetriever(RagRetrievalResult("Solution context", [solution_reference]))
    service = ReportRagService(evidence_retriever=evidence, solution_retriever=solutions, max_context_chars=500)

    result = service.retrieve(request=_request())

    assert evidence.calls == [{
        "target_type": "product",
        "target_id": "product-a",
        "query": "Delivery delays are the primary risk. Negative keywords: late delivery",
        "problem_type": "logistics",
        "language": "en-US",
    }]
    assert solutions.calls == [{
        "review_text": "Delivery delays are the primary risk. Negative keywords: late delivery",
        "review_score": None,
        "problem_type": "logistics",
        "language": "en-US",
        "source_types": {"problem_solution"},
    }]
    assert [reference.source_type for reference in result.references] == ["review_evidence", "problem_solution"]
    assert result.context == "Review evidence context\n\nSolution context"


def test_report_rag_returns_no_references_when_retrieval_fails() -> None:
    class FailingRetriever:
        def retrieve(self, **_kwargs):
            raise RuntimeError("index unavailable")

    service = ReportRagService(
        evidence_retriever=FailingRetriever(),
        solution_retriever=FailingRetriever(),
        max_context_chars=500,
    )

    result = service.retrieve(request=_request())

    assert result.context == ""
    assert result.references == []


def test_report_rag_respects_context_budget_without_orphaning_references() -> None:
    evidence_reference = RagReference("review_evidence", 31, "Review evidence #31", 0.93)
    solution_reference = RagReference("problem_solution", 8, "Delivery checklist", 0.87)
    service = ReportRagService(
        evidence_retriever=FakeEvidenceRetriever(RagRetrievalResult("A" * 80, [evidence_reference])),
        solution_retriever=FakeSolutionRetriever(RagRetrievalResult("B" * 80, [solution_reference])),
        max_context_chars=100,
    )

    result = service.retrieve(request=_request())

    assert result.context == "A" * 80
    assert result.references == [evidence_reference]


def test_report_response_exposes_only_programmatic_references() -> None:
    reference = RagReference("review_evidence", 31, "Review evidence #31", 0.93)
    service = AiService()
    service._chat = lambda _prompt, temperature: '{"reportTitle":"Evidence report"}'
    service._report_rag_service = lambda: type(
        "FakeReportRagService",
        (),
        {"retrieve": lambda _self, **_kwargs: RagRetrievalResult("Evidence context", [reference])},
    )()

    with patch("app.services.ai_service.settings", SimpleNamespace(ai_model="deepseek-chat")):
        result = service.generate_report(_request())

    assert result["data"]["ragUsed"] is True
    assert result["data"]["references"] == [reference.to_payload()]
