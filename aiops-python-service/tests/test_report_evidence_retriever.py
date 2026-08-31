from types import SimpleNamespace

from langchain_core.documents import Document

from app.rag.models import RagIndexStatus
from app.rag.report_evidence_retriever import ReportEvidenceRetriever


class FakeIndexService:
    def __init__(self, state: str = "ready") -> None:
        self.current_status = RagIndexStatus(
            enabled=state != "disabled",
            ready=state == "ready",
            state=state,
            collection="aiops_knowledge_v1",
            document_count=3,
            problem_solution_count=1,
            historical_reply_count=0,
            embedding_model="fake",
            last_reindex_at=None,
            last_error=None,
            review_evidence_count=2,
        )
        self.ensure_calls = 0

    def status(self) -> RagIndexStatus:
        return self.current_status

    def ensure_index_for_retrieval(self) -> bool:
        self.ensure_calls += 1
        return True


class FakeVectorStore:
    def __init__(self, matches) -> None:
        self.matches = matches
        self.calls = []

    def similarity_search_with_relevance_scores(self, query, *, k, filter=None):
        self.calls.append((query, k, filter))
        return self.matches


class FailingVectorStore(FakeVectorStore):
    def similarity_search_with_relevance_scores(self, query, *, k, filter=None):
        self.calls.append((query, k, filter))
        raise RuntimeError("vector store unavailable")


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


def _review(
    source_id: int,
    *,
    product_id: str = "product-1",
    seller_id: str = "seller-1",
    problem_type: str = "logistics",
    text: str = "The package arrived late.",
) -> Document:
    return Document(
        page_content=text,
        metadata={
            "sourceType": "review_evidence",
            "sourceId": source_id,
            "productId": product_id,
            "sellerId": seller_id,
            "problemType": problem_type,
            "title": f"Review evidence #{source_id}",
        },
    )


def test_product_retrieval_filters_and_excludes_cross_target_evidence() -> None:
    store = FakeVectorStore(
        [
            (_review(9, product_id="other-product"), 0.98),
            (_review(4), 0.82),
        ]
    )
    retriever = ReportEvidenceRetriever(runtime=_runtime(store), index_service=FakeIndexService())

    result = retriever.retrieve(
        target_type="product",
        target_id="product-1",
        query="Late delivery is the main customer complaint.",
        problem_type="logistics",
        language="en-US",
    )

    assert store.calls[0][2] == {
        "$and": [
            {"sourceType": "review_evidence"},
            {"productId": "product-1"},
            {"problemType": "logistics"},
        ]
    }
    assert [reference.source_id for reference in result.references] == [4]
    assert "Review evidence: The package arrived late." in result.context


def test_seller_retrieval_deduplicates_and_keeps_ties_in_source_id_order() -> None:
    store = FakeVectorStore(
        [
            (_review(8, seller_id="seller-9", text="Wrong seller."), 0.99),
            (_review(7, seller_id="seller-1", text="Second evidence."), 0.81),
            (_review(3, seller_id="seller-1", text="First evidence."), 0.81),
            (_review(3, seller_id="seller-1", text="Duplicate evidence."), 0.80),
        ]
    )
    retriever = ReportEvidenceRetriever(runtime=_runtime(store), index_service=FakeIndexService())

    result = retriever.retrieve(
        target_type="seller",
        target_id="seller-1",
        query="Customer review evidence for the seller.",
        problem_type=None,
        language="zh-CN",
    )

    assert store.calls[0][2] == {
        "$and": [
            {"sourceType": "review_evidence"},
            {"sellerId": "seller-1"},
        ]
    }
    assert [reference.source_id for reference in result.references] == [3, 7]
    assert "Duplicate evidence." not in result.context


def test_empty_index_requests_rebuild_without_loading_vector_store() -> None:
    store = FakeVectorStore([])
    index_service = FakeIndexService(state="empty")
    retriever = ReportEvidenceRetriever(runtime=_runtime(store), index_service=index_service)

    result = retriever.retrieve(
        target_type="product",
        target_id="product-1",
        query="Quality issue.",
        problem_type="quality",
        language="en-US",
    )

    assert result.context == ""
    assert result.references == []
    assert index_service.ensure_calls == 1
    assert store.calls == []


def test_disabled_rag_returns_empty_result_without_touching_dependencies() -> None:
    store = FakeVectorStore([])
    runtime = _runtime(store)
    runtime.settings.rag_enabled = False
    index_service = FakeIndexService()
    retriever = ReportEvidenceRetriever(runtime=runtime, index_service=index_service)

    result = retriever.retrieve(
        target_type="product",
        target_id="product-1",
        query="Quality issue.",
        problem_type="quality",
        language="en-US",
    )

    assert result.context == ""
    assert result.references == []
    assert index_service.ensure_calls == 0
    assert store.calls == []


def test_unavailable_vector_store_returns_no_evidence() -> None:
    store = FailingVectorStore([])
    retriever = ReportEvidenceRetriever(runtime=_runtime(store), index_service=FakeIndexService())

    result = retriever.retrieve(
        target_type="product",
        target_id="product-1",
        query="Quality issue.",
        problem_type="quality",
        language="en-US",
    )

    assert result.context == ""
    assert result.references == []
    assert len(store.calls) == 1
