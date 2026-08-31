from __future__ import annotations

import json
from typing import Any

from app.rag.knowledge_retriever import KnowledgeRetriever, knowledge_retriever
from app.rag.models import RagReference, RagRetrievalResult
from app.rag.report_evidence_retriever import ReportEvidenceRetriever, report_evidence_retriever


class ReportRagService:
    """Builds bounded, application-owned report context from approved RAG sources."""

    def __init__(
        self,
        *,
        evidence_retriever: ReportEvidenceRetriever | Any = report_evidence_retriever,
        solution_retriever: KnowledgeRetriever | Any = knowledge_retriever,
        max_context_chars: int = 6000,
    ) -> None:
        self._evidence_retriever = evidence_retriever
        self._solution_retriever = solution_retriever
        self._max_context_chars = max_context_chars

    def retrieve(self, *, request: dict[str, Any]) -> RagRetrievalResult:
        target_type = _text(request.get("targetType")).lower()
        target_id = _text(request.get("targetId"))
        if target_type not in {"product", "seller"} or not target_id:
            return _empty_result()

        analysis_result = request.get("analysisResult")
        analysis = analysis_result if isinstance(analysis_result, dict) else {}
        problem_type = _primary_problem_type(analysis.get("problemDistribution"))
        query = _query_text(analysis)
        language = _text(request.get("language")) or "zh-CN"
        try:
            evidence = self._evidence_retriever.retrieve(
                target_type=target_type,
                target_id=target_id,
                query=query,
                problem_type=problem_type,
                language=language,
            )
            solutions = self._solution_retriever.retrieve(
                review_text=query,
                review_score=None,
                problem_type=problem_type,
                language=language,
                source_types={"problem_solution"},
            )
        except Exception:
            return _empty_result()
        return _merge_results((evidence, solutions), self._max_context_chars)


def _merge_results(results: tuple[RagRetrievalResult, ...], max_context_chars: int) -> RagRetrievalResult:
    context_parts: list[str] = []
    references: list[RagReference] = []
    seen: set[tuple[str, int]] = set()
    remaining = max(0, max_context_chars)

    for result in results:
        if not result.context or not result.references:
            continue
        result_references = [
            reference
            for reference in result.references
            if (reference.source_type, reference.source_id) not in seen
        ]
        if not result_references:
            continue
        separator_length = 2 if context_parts else 0
        available = remaining - separator_length
        if len(result.context) > available:
            continue
        context_parts.append(result.context)
        references.extend(result_references)
        seen.update((reference.source_type, reference.source_id) for reference in result_references)
        remaining -= separator_length + len(result.context)

    return RagRetrievalResult(context="\n\n".join(context_parts), references=references)


def _primary_problem_type(value: Any) -> str | None:
    parsed = _json_value(value)
    if isinstance(parsed, dict):
        candidates = [(str(key), _number(score)) for key, score in parsed.items()]
    elif isinstance(parsed, list):
        candidates = [
            (_text(item.get("name") or item.get("problemType") or item.get("type")), _number(item.get("value") or item.get("count")))
            for item in parsed
            if isinstance(item, dict)
        ]
    else:
        candidates = []
    normalized = [(_text(name).lower(), score) for name, score in candidates if _text(name).lower() not in {"", "unknown"}]
    if not normalized:
        return None
    return sorted(normalized, key=lambda item: (-item[1], item[0]))[0][0]


def _query_text(analysis: dict[str, Any]) -> str:
    summary = _text(analysis.get("summary"))
    keywords = _keywords(analysis.get("negativeKeywords"))
    parts = [summary or "Operational review analysis"]
    if keywords:
        parts.append(f"Negative keywords: {', '.join(keywords)}")
    return " ".join(parts)


def _keywords(value: Any) -> list[str]:
    parsed = _json_value(value)
    if isinstance(parsed, list):
        return [
            text
            for item in parsed
            if (text := _text(item.get("name") if isinstance(item, dict) else item))
        ][:5]
    return [_text(parsed)] if _text(parsed) else []


def _json_value(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return value


def _number(value: Any) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def _text(value: Any) -> str:
    return str(value or "").strip()


def _empty_result() -> RagRetrievalResult:
    return RagRetrievalResult(context="", references=[])


report_rag_service = ReportRagService()
