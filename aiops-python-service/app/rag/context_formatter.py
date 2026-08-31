from dataclasses import dataclass

from langchain_core.documents import Document

from app.rag.models import RagReference, RagRetrievalResult


@dataclass(frozen=True)
class RetrievedKnowledge:
    document: Document
    reference: RagReference


def format_reference_context(
    matches: list[RetrievedKnowledge],
    *,
    max_context_chars: int,
) -> RagRetrievalResult:
    context_parts: list[str] = []
    references: list[RagReference] = []
    seen: set[tuple[str, int]] = set()
    remaining = max_context_chars

    for match in matches:
        reference_key = (match.reference.source_type, match.reference.source_id)
        if reference_key in seen:
            continue
        block = _context_block(match)
        separator_length = 2 if context_parts else 0
        available = remaining - separator_length
        if len(block) > available:
            if not context_parts:
                block = block[:available].rstrip()
            else:
                break
        if not block:
            break
        context_parts.append(block)
        references.append(match.reference)
        seen.add(reference_key)
        remaining -= separator_length + len(block)
        if remaining <= 0:
            break

    return RagRetrievalResult(context="\n\n".join(context_parts), references=references)


def _context_block(match: RetrievedKnowledge) -> str:
    title = match.reference.title or "Untitled guidance"
    return (
        f"Source: {match.reference.source_type} #{match.reference.source_id} - {title}\n"
        f"Operating guidance: {match.document.page_content.strip()}"
    )
