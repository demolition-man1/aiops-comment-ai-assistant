from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app
from app.rag.models import RagIndexStatus


def _status(state: str = "ready") -> RagIndexStatus:
    return RagIndexStatus(
        enabled=state != "disabled",
        ready=state == "ready",
        state=state,
        collection="aiops_knowledge_v1",
        document_count=3,
        problem_solution_count=2,
        historical_reply_count=1,
        embedding_model="intfloat/multilingual-e5-small",
        last_reindex_at="2026-08-31T10:00:00+00:00",
        last_error=None,
    )


class FakeIndexService:
    def __init__(self, *, accepted: bool = True, state: str = "ready") -> None:
        self.accepted = accepted
        self.current_status = _status(state)
        self.run_calls = 0

    def status(self) -> RagIndexStatus:
        return self.current_status

    def start_reindex(self) -> bool:
        return self.accepted

    def run_reserved_reindex(self) -> RagIndexStatus:
        self.run_calls += 1
        return self.current_status


def test_rag_status_endpoint_returns_fixed_safe_payload() -> None:
    service = FakeIndexService()
    with patch("app.routers.rag_router.rag_index_service", service):
        response = TestClient(app).get("/internal/ai/rag/status")

    assert response.status_code == 200
    assert response.json() == {
        "success": True,
        "data": {
            "enabled": True,
            "ready": True,
            "state": "ready",
            "collection": "aiops_knowledge_v1",
            "documentCount": 3,
            "problemSolutionCount": 2,
            "historicalReplyCount": 1,
            "embeddingModel": "intfloat/multilingual-e5-small",
            "lastReindexAt": "2026-08-31T10:00:00+00:00",
            "lastError": None,
        },
    }


def test_rag_reindex_endpoint_accepts_one_background_rebuild() -> None:
    service = FakeIndexService(state="building")
    with patch("app.routers.rag_router.rag_index_service", service):
        response = TestClient(app).post("/internal/ai/rag/reindex")

    assert response.status_code == 202
    assert response.json()["data"]["state"] == "building"
    assert service.run_calls == 1


def test_rag_reindex_endpoint_rejects_another_rebuild_while_one_is_running() -> None:
    service = FakeIndexService(accepted=False, state="building")
    with patch("app.routers.rag_router.rag_index_service", service):
        response = TestClient(app).post("/internal/ai/rag/reindex")

    assert response.status_code == 409
    assert response.json()["detail"] == "Knowledge index rebuild is already running."
    assert service.run_calls == 0
