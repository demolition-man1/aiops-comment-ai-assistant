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
        source_types: set[str] | None = None,
        top_k: int | None = None,
        max_context_chars: int | None = None,
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
        allowed_source_types = source_types or {"problem_solution", "historical_reply"}
        result_limit = top_k or self._runtime.settings.rag_top_k
        context_limit = max_context_chars or self._runtime.settings.rag_max_context_chars
        matches = self._runtime.get_vector_store().similarity_search_with_relevance_scores(
            _query_text(
                review_text=review_text,
                review_score=review_score,
                problem_type=normalized_problem_type,
                language=language,
            ),
            k=result_limit,
            filter=_metadata_filter(normalized_problem_type, allowed_source_types),
        )
        knowledge = _valid_matches(
            matches,
            minimum_score=self._runtime.settings.rag_min_relevance_score,
            problem_type=normalized_problem_type,
            allowed_source_types=allowed_source_types,
        )
        knowledge.sort(key=lambda item: item.reference.score, reverse=True)
        return format_reference_context(
            knowledge[:result_limit],
            max_context_chars=context_limit,
        )


def _valid_matches(
    matches: list[tuple[Document, float]],
    *,
    minimum_score: float,
    problem_type: str | None,
    allowed_source_types: set[str],
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
        reference = _reference_from_metadata(document.metadata, float(score), allowed_source_types)
        if reference is None:
            continue
        document_problem_type = _known_problem_type(document.metadata.get("problemType"))
        if problem_type and document_problem_type != problem_type:
            continue
        valid.append(RetrievedKnowledge(document=document, reference=reference))
    return valid


def _reference_from_metadata(
    metadata: dict[str, Any],
    score: float,
    allowed_source_types: set[str],
) -> RagReference | None:
    source_type = metadata.get("sourceType")
    if source_type not in allowed_source_types:
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


def _metadata_filter(problem_type: str | None, source_types: set[str]) -> dict[str, Any] | None:
    default_source_types = {"problem_solution", "historical_reply"}
    conditions: list[dict[str, Any]] = []
    if source_types != default_source_types:
        if len(source_types) == 1:
            conditions.append({"sourceType": next(iter(source_types))})
        else:
            conditions.append({"sourceType": {"$in": sorted(source_types)}})
    if problem_type:
        conditions.append({"problemType": problem_type})
    if not conditions:
        return None
    return conditions[0] if len(conditions) == 1 else {"$and": conditions}


def _empty_result() -> RagRetrievalResult:
    return RagRetrievalResult(context="", references=[])


knowledge_retriever = KnowledgeRetriever()
