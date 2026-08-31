from langchain_core.documents import Document

from app.rag.context_formatter import RetrievedKnowledge, format_reference_context
from app.rag.models import RagReference


def _item(source_id: int, title: str, content: str) -> RetrievedKnowledge:
    return RetrievedKnowledge(
        document=Document(page_content=content, metadata={}),
        reference=RagReference(
            source_type="problem_solution",
            source_id=source_id,
            title=title,
            score=0.9,
        ),
    )


def test_context_formatter_keeps_first_reference_once_in_stable_order() -> None:
    result = format_reference_context(
        [_item(14, "Delivery guide", "Confirm the carrier timeline."), _item(14, "Duplicate", "Ignore this.")],
        max_context_chars=500,
    )

    assert [reference.source_id for reference in result.references] == [14]
    assert "Source: problem_solution #14 - Delivery guide" in result.context
    assert "Confirm the carrier timeline." in result.context
    assert "Ignore this." not in result.context


def test_context_formatter_limits_context_without_returning_unincluded_sources() -> None:
    result = format_reference_context(
        [_item(1, "First", "A" * 140), _item(2, "Second", "B" * 140)],
        max_context_chars=180,
    )

    assert len(result.context) <= 180
    assert [reference.source_id for reference in result.references] == [1]


def test_context_formatter_counts_separator_characters_toward_the_limit() -> None:
    first = _item(1, "First", "Short first guidance.")
    second = _item(2, "Second", "Short second guidance.")
    first_context = format_reference_context([first], max_context_chars=500).context
    second_context = format_reference_context([second], max_context_chars=500).context

    result = format_reference_context(
        [first, second],
        max_context_chars=len(first_context) + len(second_context) + 1,
    )

    assert len(result.context) <= len(first_context) + len(second_context) + 1
