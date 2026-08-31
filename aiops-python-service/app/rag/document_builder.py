from __future__ import annotations

from datetime import date, datetime
from typing import Any

from langchain_core.documents import Document


def build_knowledge_documents(
    *,
    problem_solutions: list[dict[str, Any]],
    historical_replies: list[dict[str, Any]],
    review_evidence: list[dict[str, Any]] | None = None,
) -> list[Document]:
    documents = [_problem_solution_document(row) for row in problem_solutions]
    documents.extend(_historical_reply_document(row) for row in historical_replies)
    documents.extend(
        document
        for row in review_evidence or []
        if (document := _review_evidence_document(row)) is not None
    )
    return documents


def _problem_solution_document(row: dict[str, Any]) -> Document:
    source_id = _required_id(row)
    problem_type = _text(row.get("problem_type"))
    category_name = _text(row.get("category_name_en"))
    title = _text(row.get("solution_title"))
    guidance = _text(row.get("solution_content"))
    keywords = _text(row.get("keywords"))
    metadata = {
        "sourceType": "problem_solution",
        "sourceId": source_id,
        "problemType": problem_type,
        "categoryNameEn": category_name,
        "recordSourceType": _text(row.get("source_type")),
        "title": title,
        "priority": _integer(row.get("priority")),
        "updatedAt": _timestamp(row.get("update_time")),
    }
    return Document(
        id=f"problem_solution:{source_id}",
        page_content="\n".join(
            (
                f"Problem type: {problem_type}",
                f"Category: {category_name}",
                f"Title: {title}",
                f"Guidance: {guidance}",
                f"Keywords: {keywords}",
            )
        ),
        metadata=metadata,
    )


def _historical_reply_document(row: dict[str, Any]) -> Document:
    source_id = _required_id(row)
    problem_type = _text(row.get("problem_type"))
    customer_issue = _text(row.get("comment_content"))
    approved_reply = _text(row.get("reply_content"))
    effect_tag = _text(row.get("effect_tag"))
    metadata = {
        "sourceType": "historical_reply",
        "sourceId": source_id,
        "problemType": problem_type,
        "title": "已确认有效的历史回复",
        "effectTag": effect_tag,
        "favoriteFlag": _integer(row.get("favorite_flag")),
        "updatedAt": _timestamp(row.get("update_time")),
    }
    return Document(
        id=f"historical_reply:{source_id}",
        page_content="\n".join(
            (
                f"Problem type: {problem_type}",
                f"Customer issue: {customer_issue}",
                f"Approved reply: {approved_reply}",
                f"Recorded outcome: {effect_tag}",
            )
        ),
        metadata=metadata,
    )


def _review_evidence_document(row: dict[str, Any]) -> Document | None:
    content = _meaningful_text(row.get("clean_content")) or _meaningful_text(row.get("review_content"))
    if content is None:
        return None
    source_id = _required_id(row)
    product_id = _text(row.get("product_id"))
    seller_id = _text(row.get("seller_id"))
    review_score = _integer(row.get("review_score"))
    sentiment = _text(row.get("sentiment"))
    problem_type = _text(row.get("problem_type"))
    review_time = _timestamp(row.get("review_time"))
    metadata = {
        "sourceType": "review_evidence",
        "sourceId": source_id,
        "productId": product_id,
        "sellerId": seller_id,
        "reviewScore": review_score,
        "sentiment": sentiment,
        "problemType": problem_type,
        "reviewTime": review_time,
        "title": f"Review evidence #{source_id}",
        "updatedAt": _timestamp(row.get("update_time")),
    }
    return Document(
        id=f"review_evidence:{source_id}",
        page_content="\n".join(
            (
                f"Product ID: {product_id}",
                f"Seller ID: {seller_id}",
                f"Review score: {review_score}",
                f"Sentiment: {sentiment}",
                f"Problem type: {problem_type}",
                f"Review time: {review_time}",
                f"Review evidence: {content}",
            )
        ),
        metadata=metadata,
    )


def _required_id(row: dict[str, Any]) -> int:
    value = row.get("id")
    if value is None:
        raise ValueError("Knowledge source id is required")
    return int(value)


def _text(value: Any) -> str:
    return "" if value is None else str(value).strip()


def _meaningful_text(value: Any) -> str | None:
    text = _text(value)
    compact = "".join(text.split()).lower()
    if not compact or compact in {"null", "none", "nan", "nannan"}:
        return None
    return text


def _integer(value: Any) -> int:
    return 0 if value is None else int(value)


def _timestamp(value: Any) -> str:
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    return _text(value)
