from contextlib import contextmanager
import json
from pathlib import Path
from threading import Event
import time
from types import SimpleNamespace

from app.rag.index_service import RagIndexService
from app.rag.runtime import RagRuntime


class FakeVectorStore:
    def __init__(self, initial_ids: list[str] | None = None, fail_add: bool = False) -> None:
        self.ids = list(initial_ids or [])
        self.added: list[tuple[list[object], list[str]]] = []
        self.deleted: list[list[str]] = []
        self.fail_add = fail_add

    def get(self, include=None) -> dict[str, list[str]]:
        return {"ids": list(self.ids)}

    def add_documents(self, documents, ids: list[str]) -> None:
        if self.fail_add:
            raise RuntimeError("simulated upsert failure")
        self.added.append((list(documents), list(ids)))
        self.ids = list(dict.fromkeys([*self.ids, *ids]))

    def delete(self, ids: list[str]) -> None:
        self.deleted.append(list(ids))
        self.ids = [item for item in self.ids if item not in ids]


class FakeRuntime:
    def __init__(self, directory: Path, store: FakeVectorStore, enabled: bool = True) -> None:
        self._settings = SimpleNamespace(
            rag_enabled=enabled,
            rag_collection="aiops_knowledge_v1",
            rag_chroma_dir=str(directory),
            embedding_model="intfloat/multilingual-e5-small",
        )
        self.store = store
        self.client_calls = 0
        self.store_calls = 0

    def get_chroma_client(self):
        self.client_calls += 1
        return SimpleNamespace(
            list_collections=lambda: [
                SimpleNamespace(name=self._settings.rag_collection, count=lambda: len(self.store.ids))
            ]
        )

    def get_vector_store(self):
        self.store_calls += 1
        return self.store


@contextmanager
def _connection():
    yield object()


def _problem_solution_rows() -> list[dict[str, object]]:
    return [
        {
            "id": 14,
            "problem_type": "logistics",
            "category_name_en": "health_beauty",
            "solution_title": "Follow shipping progress",
            "solution_content": "Confirm the carrier timeline and update the customer.",
            "keywords": "delivery,shipping",
            "source_type": "merchant",
            "priority": 10,
            "update_time": "2026-08-30T10:00:00",
        }
    ]


def _historical_reply_rows() -> list[dict[str, object]]:
    return [
        {
            "id": 25,
            "problem_type": "logistics",
            "comment_content": "Delivery was delayed.",
            "reply_content": "We are checking the delivery status for you.",
            "effect_tag": "resolved",
            "favorite_flag": 0,
            "update_time": "2026-08-30T10:05:00",
        }
    ]


def _service(tmp_path: Path, store: FakeVectorStore, *, enabled: bool = True) -> RagIndexService:
    return RagIndexService(
        runtime=FakeRuntime(tmp_path, store, enabled=enabled),
        connection_factory=_connection,
        problem_solution_loader=lambda conn: _problem_solution_rows(),
        historical_reply_loader=lambda conn: _historical_reply_rows(),
    )


def test_disabled_status_does_not_initialize_chroma(tmp_path: Path) -> None:
    runtime = FakeRuntime(tmp_path, FakeVectorStore(), enabled=False)
    service = RagIndexService(runtime=runtime, connection_factory=_connection)

    status = service.status()

    assert status.state == "disabled"
    assert not status.enabled
    assert runtime.client_calls == 0
    assert runtime.store_calls == 0


def test_reindex_upserts_current_documents_then_removes_stale_documents(tmp_path: Path) -> None:
    store = FakeVectorStore(initial_ids=["problem_solution:99", "unrelated:1"])
    service = _service(tmp_path, store)

    status = service.reindex()

    assert status.state == "ready"
    assert status.ready
    assert status.document_count == 2
    assert status.problem_solution_count == 1
    assert status.historical_reply_count == 1
    assert store.added[0][1] == ["problem_solution:14", "historical_reply:25"]
    assert store.deleted == [["problem_solution:99"]]
    assert store.ids == ["unrelated:1", "problem_solution:14", "historical_reply:25"]
    state = json.loads((tmp_path / "index-state.json").read_text(encoding="utf-8"))
    assert state["state"] == "ready"
    assert state["documentCount"] == 2


def test_failed_upsert_preserves_previous_documents_and_records_safe_status(tmp_path: Path) -> None:
    store = FakeVectorStore(initial_ids=["problem_solution:99"], fail_add=True)
    service = _service(tmp_path, store)

    status = service.reindex()

    assert status.state == "failed"
    assert store.ids == ["problem_solution:99"]
    assert store.deleted == []
    assert status.last_error == "Knowledge index rebuild failed."


def test_same_source_rows_replace_existing_ids_without_creating_duplicates(tmp_path: Path) -> None:
    store = FakeVectorStore()
    service = _service(tmp_path, store)

    first_status = service.reindex()
    second_status = service.reindex()

    assert first_status.document_count == 2
    assert second_status.document_count == 2
    assert store.ids == ["problem_solution:14", "historical_reply:25"]


def test_recreated_service_reads_persisted_index_state(tmp_path: Path) -> None:
    store = FakeVectorStore()
    _service(tmp_path, store).reindex()

    status = _service(tmp_path, store).status()

    assert status.state == "ready"
    assert status.document_count == 2
    assert status.problem_solution_count == 1
    assert status.historical_reply_count == 1


def test_unavailable_chroma_client_reports_a_safe_failed_status(tmp_path: Path) -> None:
    runtime = FakeRuntime(tmp_path, FakeVectorStore())
    runtime.get_chroma_client = lambda: (_ for _ in ()).throw(RuntimeError("storage unavailable"))
    service = RagIndexService(runtime=runtime, connection_factory=_connection)

    status = service.status()

    assert status.state == "failed"
    assert status.last_error == "Knowledge index is unavailable."


def test_empty_index_can_start_one_background_rebuild_without_blocking_request(tmp_path: Path) -> None:
    source_started = Event()
    allow_source_return = Event()
    runtime = FakeRuntime(tmp_path, FakeVectorStore())

    def slow_problem_solution_loader(conn):
        source_started.set()
        allow_source_return.wait(timeout=2)
        return _problem_solution_rows()

    service = RagIndexService(
        runtime=runtime,
        connection_factory=_connection,
        problem_solution_loader=slow_problem_solution_loader,
        historical_reply_loader=lambda conn: [],
    )

    started_at = time.monotonic()
    assert service.ensure_index_for_retrieval()
    assert time.monotonic() - started_at < 0.5
    assert source_started.wait(timeout=1)
    assert service.status().state == "building"

    allow_source_return.set()
    service.wait_for_background_reindex(timeout=2)
    assert service.status().state == "ready"


def test_real_chroma_sync_works_with_fake_embeddings_and_temporary_storage(tmp_path: Path) -> None:
    class FakeEmbeddings:
        def embed_documents(self, texts: list[str]) -> list[list[float]]:
            return [[float(len(text)), 1.0] for text in texts]

        def embed_query(self, text: str) -> list[float]:
            return [float(len(text)), 1.0]

    settings = SimpleNamespace(
        rag_enabled=True,
        rag_collection="aiops_knowledge_v1",
        rag_chroma_dir=str(tmp_path / "chroma"),
        embedding_model="fake-embeddings",
        embedding_device="cpu",
    )
    runtime = RagRuntime(
        settings,
        embeddings_factory=FakeEmbeddings,
    )
    service = RagIndexService(
        runtime=runtime,
        connection_factory=_connection,
        problem_solution_loader=lambda conn: _problem_solution_rows(),
        historical_reply_loader=lambda conn: [],
    )

    status = service.reindex()

    assert status.state == "ready"
    assert status.document_count == 1
    assert (tmp_path / "chroma" / "index-state.json").exists()
