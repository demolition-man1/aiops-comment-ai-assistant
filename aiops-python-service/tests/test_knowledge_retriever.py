from types import SimpleNamespace

from langchain_core.documents import Document

from app.rag.knowledge_retriever import KnowledgeRetriever
from app.rag.models import RagIndexStatus


class FakeIndexService:
    def __init__(self, state: str = "ready") -> None:
        self.current_status = RagIndexStatus(
            enabled=state != "disabled",
            ready=state == "ready",
            state=state,
            collection="aiops_knowledge_v1",
            document_count=2,
            problem_solution_count=1,
            historical_reply_count=1,
            embedding_model="fake",
            last_reindex_at=None,
            last_error=None,
        )
        self.ensure_calls = 0

    def status(self) -> RagIndexStatus:
        return self.current_status

    def ensure_index_for_retrieval(self) -> bool:
        self.ensure_calls += 1
        return True


class FakeVectorStore:
    def __init__(self, matches):
        self.matches = matches
        self.calls: list[tuple[str, int, dict[str, str] | None]] = []

    def similarity_search_with_relevance_scores(self, query, *, k, filter=None):
        self.calls.append((query, k, filter))
        return self.matches


def _runtime(store: FakeVectorStore) -> SimpleNamespace:
    return SimpleNamespace(
        settings=SimpleNamespace(
            rag_enabled=True,
            rag_top_k=3,
            rag_min_relevance_score=0.5,
            rag_max_context_chars=600,
        ),
        get_vector_store=lambda: store,
    )


def _document(source_id: int, problem_type: str, title: str) -> Document:
    return Document(
        page_content=f"Guidance for {title}",
        metadata={
            "sourceType": "problem_solution",
            "sourceId": source_id,
            "problemType": problem_type,
            "title": title,
        },
    )


def test_known_problem_type_applies_chroma_filter_and_keeps_ranked_references() -> None:
    store = FakeVectorStore(
        [
            (_document(4, "quality", "Unrelated quality guide"), 0.64),
            (_document(14, "logistics", "Delivery guide"), 0.91),
        ]
    )
    retriever = KnowledgeRetriever(runtime=_runtime(store), index_service=FakeIndexService())

    result = retriever.retrieve(
        review_text="The package arrived late.",
        review_score=1,
        problem_type="logistics",
        language="en-US",
    )

    assert store.calls[0][2] == {"problemType": "logistics"}
    assert [reference.source_id for reference in result.references] == [14]
    assert [reference.score for reference in result.references] == [0.91]
    assert "Current customer review: The package arrived late." in store.calls[0][0]


def test_unknown_problem_type_avoids_filter_and_drops_low_or_malformed_results() -> None:
    malformed = Document(page_content="unsafe", metadata={"sourceType": "problem_solution", "sourceId": "bad"})
    store = FakeVectorStore(
        [
            (_document(1, "logistics", "High confidence"), 0.9),
            (_document(2, "quality", "Low confidence"), 0.2),
            (_document(3, "quality", "Invalid score"), float("nan")),
            (malformed, 0.8),
        ]
    )
    retriever = KnowledgeRetriever(runtime=_runtime(store), index_service=FakeIndexService())

    result = retriever.retrieve(
        review_text="Atrasou muito.",
        review_score=None,
        problem_type="unknown",
        language="pt-BR",
    )

    assert store.calls[0][2] is None
    assert [reference.source_id for reference in result.references] == [1]


def test_empty_index_starts_background_sync_without_initializing_vector_store() -> None:
    index_service = FakeIndexService(state="empty")
    store = FakeVectorStore([])
    retriever = KnowledgeRetriever(runtime=_runtime(store), index_service=index_service)

    result = retriever.retrieve(
        review_text="Produto quebrado.",
        review_score=1,
        problem_type="quality",
        language="pt-BR",
    )

    assert result.context == ""
    assert result.references == []
    assert index_service.ensure_calls == 1
    assert store.calls == []


def test_disabled_rag_returns_empty_result_without_touching_index_or_vector_store() -> None:
    store = FakeVectorStore([])
    runtime = _runtime(store)
    runtime.settings.rag_enabled = False
    index_service = FakeIndexService()
    retriever = KnowledgeRetriever(runtime=runtime, index_service=index_service)

    result = retriever.retrieve(
        review_text="Late delivery.",
        review_score=1,
        problem_type="logistics",
        language="en-US",
    )

    assert result == type(result)(context="", references=[])
    assert index_service.ensure_calls == 0
    assert store.calls == []
