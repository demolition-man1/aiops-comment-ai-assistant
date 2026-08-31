from __future__ import annotations

import math
from typing import Any

from langchain_core.documents import Document

from app.rag.context_formatter import RetrievedKnowledge, format_reference_context
from app.rag.index_service import RagIndexService, rag_index_service
from app.rag.models import RagReference, RagRetrievalResult
from app.rag.runtime import RagRuntime, rag_runtime


class KnowledgeRetriever:
    def __init__(
        self,
        *,
        runtime: RagRuntime | Any = rag_runtime,
        index_service: RagIndexService | Any = rag_index_service,
    ) -> None:
        self._runtime = runtime
        self._index_service = index_service

    def retrieve(
        self,
        *,
        review_text: str,
        review_score: int | None,
        problem_type: str | None,
        language: str,
    ) -> RagRetrievalResult:
        if not self._runtime.settings.rag_enabled:
            return _empty_result()

        status = self._index_service.status()
        if status.state == "empty":
            self._index_service.ensure_index_for_retrieval()
            return _empty_result()
        if status.state != "ready":
            return _empty_result()

        normalized_problem_type = _known_problem_type(problem_type)
        matches = self._runtime.get_vector_store().similarity_search_with_relevance_scores(
            _query_text(
                review_text=review_text,
                review_score=review_score,
                problem_type=normalized_problem_type,
                language=language,
            ),
            k=self._runtime.settings.rag_top_k,
            filter={"problemType": normalized_problem_type} if normalized_problem_type else None,
        )
        knowledge = _valid_matches(
            matches,
            minimum_score=self._runtime.settings.rag_min_relevance_score,
            problem_type=normalized_problem_type,
        )
        knowledge.sort(key=lambda item: item.reference.score, reverse=True)
        return format_reference_context(
            knowledge[: self._runtime.settings.rag_top_k],
            max_context_chars=self._runtime.settings.rag_max_context_chars,
        )


def _valid_matches(
    matches: list[tuple[Document, float]],
    *,
    minimum_score: float,
    problem_type: str | None,
) -> list[RetrievedKnowledge]:
    valid: list[RetrievedKnowledge] = []
    for document, score in matches:
        if (
            not isinstance(document, Document)
            or not isinstance(score, (int, float))
            or not math.isfinite(score)
            or score < minimum_score
        ):
            continue
        reference = _reference_from_metadata(document.metadata, float(score))
        if reference is None:
            continue
        document_problem_type = _known_problem_type(document.metadata.get("problemType"))
        if problem_type and document_problem_type != problem_type:
            continue
        valid.append(RetrievedKnowledge(document=document, reference=reference))
    return valid


def _reference_from_metadata(metadata: dict[str, Any], score: float) -> RagReference | None:
    source_type = metadata.get("sourceType")
    if source_type not in {"problem_solution", "historical_reply"}:
        return None
    try:
        source_id = int(metadata.get("sourceId"))
    except (TypeError, ValueError):
        return None
    if source_id < 1:
        return None
    title = str(metadata.get("title") or "").strip() or None
    return RagReference(source_type=source_type, source_id=source_id, title=title, score=score)


def _known_problem_type(value: Any) -> str | None:
    text = str(value or "").strip().lower()
    return text if text and text != "unknown" else None


def _query_text(
    *,
    review_text: str,
    review_score: int | None,
    problem_type: str | None,
    language: str,
) -> str:
    return "\n".join(
        (
            f"Language: {language or 'unknown'}",
            f"Review score: {review_score if review_score is not None else 'unknown'}",
            f"Problem type: {problem_type or 'unknown'}",
            f"Current customer review: {review_text.strip()}",
        )
    )


def _empty_result() -> RagRetrievalResult:
    return RagRetrievalResult(context="", references=[])


knowledge_retriever = KnowledgeRetriever()
