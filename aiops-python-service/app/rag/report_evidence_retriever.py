from __future__ import annotations

import math
from typing import Any, Literal

from langchain_core.documents import Document

from app.rag.context_formatter import RetrievedKnowledge, format_reference_context
from app.rag.index_service import RagIndexService, rag_index_service
from app.rag.models import RagReference, RagRetrievalResult
from app.rag.runtime import RagRuntime, rag_runtime


TargetType = Literal["product", "seller"]


class ReportEvidenceRetriever:
    """Retrieves review evidence constrained to one report target."""

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
        target_type: TargetType | str,
        target_id: str,
        query: str,
        problem_type: str | None,
        language: str,
    ) -> RagRetrievalResult:
        target_key = _target_metadata_key(target_type)
        normalized_target_id = _text(target_id)
        if target_key is None or not normalized_target_id or not self._runtime.settings.rag_enabled:
            return _empty_result()

        try:
            status = self._index_service.status()
            if status.state == "empty":
                self._index_service.ensure_index_for_retrieval()
                return _empty_result()
            if status.state != "ready":
                return _empty_result()

            normalized_problem_type = _known_problem_type(problem_type)
            matches = self._runtime.get_vector_store().similarity_search_with_relevance_scores(
                _query_text(
                    target_type=target_type,
                    target_id=normalized_target_id,
                    query=query,
                    problem_type=normalized_problem_type,
                    language=language,
                ),
                k=self._runtime.settings.rag_top_k,
                filter=_metadata_filter(
                    target_key=target_key,
                    target_id=normalized_target_id,
                    problem_type=normalized_problem_type,
                ),
            )
        except Exception:
            return _empty_result()

        knowledge = _valid_matches(
            matches,
            minimum_score=self._runtime.settings.rag_min_relevance_score,
            target_key=target_key,
            target_id=normalized_target_id,
            problem_type=normalized_problem_type,
        )
        knowledge.sort(key=lambda item: (-item.reference.score, item.reference.source_id))
        return format_reference_context(
            knowledge[: self._runtime.settings.rag_top_k],
            max_context_chars=self._runtime.settings.rag_max_context_chars,
        )


def _valid_matches(
    matches: list[tuple[Document, float]],
    *,
    minimum_score: float,
    target_key: str,
    target_id: str,
    problem_type: str | None,
) -> list[RetrievedKnowledge]:
    valid: list[RetrievedKnowledge] = []
    for document, score in matches:
        if (
            not isinstance(document, Document)
            or not isinstance(score, (int, float))
            or not math.isfinite(score)
            or score < minimum_score
            or document.metadata.get("sourceType") != "review_evidence"
            or _text(document.metadata.get(target_key)) != target_id
        ):
            continue
        document_problem_type = _known_problem_type(document.metadata.get("problemType"))
        if problem_type and document_problem_type != problem_type:
            continue
        reference = _reference_from_metadata(document.metadata, float(score))
        if reference is not None:
            valid.append(RetrievedKnowledge(document=document, reference=reference))
    return valid


def _reference_from_metadata(metadata: dict[str, Any], score: float) -> RagReference | None:
    try:
        source_id = int(metadata.get("sourceId"))
    except (TypeError, ValueError):
        return None
    if source_id < 1:
        return None
    title = _text(metadata.get("title")) or None
    return RagReference(
        source_type="review_evidence",
        source_id=source_id,
        title=title,
        score=score,
    )


def _metadata_filter(*, target_key: str, target_id: str, problem_type: str | None) -> dict[str, list[dict[str, str]]]:
    conditions = [
        {"sourceType": "review_evidence"},
        {target_key: target_id},
    ]
    if problem_type:
        conditions.append({"problemType": problem_type})
    return {"$and": conditions}


def _query_text(
    *,
    target_type: str,
    target_id: str,
    query: str,
    problem_type: str | None,
    language: str,
) -> str:
    return "\n".join(
        (
            f"Language: {_text(language) or 'unknown'}",
            f"Report target: {_text(target_type)} {_text(target_id)}",
            f"Problem type: {problem_type or 'unknown'}",
            f"Analysis focus: {_text(query)}",
        )
    )


def _target_metadata_key(target_type: str) -> str | None:
    normalized = _text(target_type).lower()
    return {"product": "productId", "seller": "sellerId"}.get(normalized)


def _known_problem_type(value: Any) -> str | None:
    text = _text(value).lower()
    return text if text and text != "unknown" else None


def _text(value: Any) -> str:
    return str(value or "").strip()


def _empty_result() -> RagRetrievalResult:
    return RagRetrievalResult(context="", references=[])


report_evidence_retriever = ReportEvidenceRetriever()
