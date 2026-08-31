from __future__ import annotations

from collections.abc import Callable
from contextlib import AbstractContextManager
from datetime import datetime, timezone
import json
import os
from pathlib import Path
from threading import RLock, Thread
from typing import Any

from app.db import get_conn
from app.rag.document_builder import build_knowledge_documents
from app.rag.models import RagIndexStatus
from app.rag.runtime import RagRuntime, rag_runtime
from app.repositories.negative_reply_repository import list_eligible_historical_replies
from app.repositories.problem_solution_repository import list_enabled_problem_solutions
from app.repositories.review_evidence_repository import list_review_evidence


class RagReindexInProgressError(RuntimeError):
    pass


class RagDisabledError(RuntimeError):
    pass


ConnectionFactory = Callable[[], AbstractContextManager[Any]]
SourceLoader = Callable[[Any], list[dict[str, Any]]]
ReviewEvidenceLoader = Callable[[Any, int], list[dict[str, Any]]]


class RagIndexService:
    def __init__(
        self,
        *,
        runtime: RagRuntime | Any = rag_runtime,
        connection_factory: ConnectionFactory = get_conn,
        problem_solution_loader: SourceLoader = list_enabled_problem_solutions,
        historical_reply_loader: SourceLoader = list_eligible_historical_replies,
        review_evidence_loader: ReviewEvidenceLoader = list_review_evidence,
    ) -> None:
        self._runtime = runtime
        self._connection_factory = connection_factory
        self._problem_solution_loader = problem_solution_loader
        self._historical_reply_loader = historical_reply_loader
        self._review_evidence_loader = review_evidence_loader
        self._state_lock = RLock()
        self._building = False
        self._background_thread: Thread | None = None

    def status(self) -> RagIndexStatus:
        settings = self._settings
        if not settings.rag_enabled:
            return self._status(state="disabled")

        saved_state = self._read_state()
        with self._state_lock:
            building = self._building
        if building:
            return self._status(state="building", persisted=saved_state)
        if saved_state.get("state") == "failed":
            return self._status(state="failed", persisted=saved_state)

        try:
            count = self._collection_count()
        except Exception:
            return self._status(
                state="failed",
                persisted=saved_state,
                last_error="Knowledge index is unavailable.",
            )
        if count > 0:
            return self._status(state="ready", document_count=count, persisted=saved_state)
        return self._status(state="empty", document_count=0, persisted=saved_state)

    def start_reindex(self) -> bool:
        if not self._settings.rag_enabled:
            raise RagDisabledError("RAG is disabled.")
        with self._state_lock:
            if self._building:
                return False
            self._building = True
            return True

    def reindex(self) -> RagIndexStatus:
        if not self.start_reindex():
            raise RagReindexInProgressError("Knowledge index rebuild is already running.")
        return self.run_reserved_reindex()

    def ensure_index_for_retrieval(self) -> bool:
        if self.status().state != "empty" or not self.start_reindex():
            return False
        thread = Thread(target=self.run_reserved_reindex, name="rag-index-rebuild", daemon=True)
        with self._state_lock:
            self._background_thread = thread
        try:
            thread.start()
        except RuntimeError:
            with self._state_lock:
                self._building = False
                self._background_thread = None
            return False
        return True

    def wait_for_background_reindex(self, timeout: float | None = None) -> None:
        with self._state_lock:
            thread = self._background_thread
        if thread is not None:
            thread.join(timeout=timeout)

    def run_reserved_reindex(self) -> RagIndexStatus:
        try:
            with self._connection_factory() as conn:
                problem_solutions = self._problem_solution_loader(conn)
                historical_replies = self._historical_reply_loader(conn)
                review_evidence = self._review_evidence_loader(conn, limit=self._review_evidence_limit)
            documents = build_knowledge_documents(
                problem_solutions=problem_solutions,
                historical_replies=historical_replies,
                review_evidence=review_evidence,
            )
            document_ids = [str(document.id) for document in documents]
            vector_store = self._runtime.get_vector_store()
            existing_ids = self._indexed_document_ids(vector_store)

            if documents:
                vector_store.add_documents(documents, ids=document_ids)

            stale_ids = sorted(existing_ids - set(document_ids))
            if stale_ids:
                vector_store.delete(ids=stale_ids)

            state = "ready" if documents else "empty"
            status = self._status(
                state=state,
                document_count=len(documents),
                problem_solution_count=len(problem_solutions),
                historical_reply_count=len(historical_replies),
                review_evidence_count=len(review_evidence),
                last_reindex_at=datetime.now(timezone.utc).isoformat(),
            )
            self._write_state(status)
            return status
        except Exception:
            status = self._status(
                state="failed",
                last_error="Knowledge index rebuild failed.",
            )
            self._write_state(status)
            return status
        finally:
            with self._state_lock:
                self._building = False

    @property
    def _settings(self) -> Any:
        return getattr(self._runtime, "settings", self._runtime._settings)

    @property
    def _state_path(self) -> Path:
        return Path(self._settings.rag_chroma_dir) / "index-state.json"

    @property
    def _review_evidence_limit(self) -> int:
        return max(0, int(getattr(self._settings, "rag_review_evidence_max_documents", 0)))

    def _collection_count(self) -> int:
        client = self._runtime.get_chroma_client()
        for collection in client.list_collections():
            if getattr(collection, "name", collection) == self._settings.rag_collection:
                return int(collection.count())
        return 0

    @staticmethod
    def _indexed_document_ids(vector_store: Any) -> set[str]:
        result = vector_store.get(include=[])
        return {
            str(value)
            for value in result.get("ids", [])
            if str(value).startswith(("problem_solution:", "historical_reply:", "review_evidence:"))
        }

    def _status(
        self,
        *,
        state: str,
        document_count: int | None = None,
        problem_solution_count: int | None = None,
        historical_reply_count: int | None = None,
        review_evidence_count: int | None = None,
        last_reindex_at: str | None = None,
        last_error: str | None = None,
        persisted: dict[str, Any] | None = None,
    ) -> RagIndexStatus:
        persisted = persisted or {}
        count = document_count if document_count is not None else _as_int(persisted.get("documentCount"))
        return RagIndexStatus(
            enabled=self._settings.rag_enabled,
            ready=state == "ready",
            state=state,
            collection=self._settings.rag_collection,
            document_count=count,
            problem_solution_count=(
                problem_solution_count
                if problem_solution_count is not None
                else _as_int(persisted.get("problemSolutionCount"))
            ),
            historical_reply_count=(
                historical_reply_count
                if historical_reply_count is not None
                else _as_int(persisted.get("historicalReplyCount"))
            ),
            review_evidence_count=(
                review_evidence_count
                if review_evidence_count is not None
                else _as_int(persisted.get("reviewEvidenceCount"))
            ),
            embedding_model=self._settings.embedding_model,
            last_reindex_at=(
                last_reindex_at if last_reindex_at is not None else _as_optional_text(persisted.get("lastReindexAt"))
            ),
            last_error=last_error if last_error is not None else _as_optional_text(persisted.get("lastError")),
        )

    def _read_state(self) -> dict[str, Any]:
        try:
            return json.loads(self._state_path.read_text(encoding="utf-8"))
        except (OSError, ValueError, TypeError):
            return {}

    def _write_state(self, status: RagIndexStatus) -> None:
        path = self._state_path
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary_path = path.with_name(f"{path.name}.tmp")
        temporary_path.write_text(
            json.dumps(status.to_payload(), ensure_ascii=False, separators=(",", ":")),
            encoding="utf-8",
        )
        os.replace(temporary_path, path)


def _as_int(value: Any) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def _as_optional_text(value: Any) -> str | None:
    text = str(value).strip() if value is not None else ""
    return text or None


rag_index_service = RagIndexService()
