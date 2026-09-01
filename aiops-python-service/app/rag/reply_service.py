from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.ai.chains.negative_reply import NegativeReplyChain
from app.ai.provider import LangChainProvider
from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput
from app.config import settings
from app.rag.knowledge_retriever import KnowledgeRetriever, knowledge_retriever
from app.rag.models import RagReference, RagRetrievalResult


@dataclass(frozen=True)
class RagReplyResult:
    invocation: AiInvocationResult[NegativeReplyOutput]
    rag_used: bool
    references: list[RagReference]


class RagReplyService:
    def __init__(
        self,
        *,
        retriever: KnowledgeRetriever | Any = knowledge_retriever,
        reply_chain: NegativeReplyChain | Any | None = None,
        reply_top_k: int | None = None,
        reply_max_context_chars: int | None = None,
    ) -> None:
        self._retriever = retriever
        self._reply_chain = reply_chain or NegativeReplyChain(LangChainProvider())
        self._reply_top_k = reply_top_k or settings.rag_reply_top_k
        self._reply_max_context_chars = reply_max_context_chars or settings.rag_reply_max_context_chars

    def generate(self, *, request: dict[str, Any], rendered_prompt: str) -> RagReplyResult:
        retrieval = self._retrieve_or_empty(request)
        use_rag = bool(retrieval.context and retrieval.references)
        invocation = self._reply_chain.generate(
            rendered_prompt,
            reference_context=retrieval.context if use_rag else None,
        )
        return RagReplyResult(
            invocation=invocation,
            rag_used=use_rag,
            references=retrieval.references if use_rag else [],
        )

    def _retrieve_or_empty(self, request: dict[str, Any]) -> RagRetrievalResult:
        try:
            return self._retriever.retrieve(
                review_text=str(request.get("commentContent") or ""),
                review_score=_review_score(request.get("reviewScore")),
                problem_type=_optional_text(request.get("problemType")),
                language=_optional_text(request.get("language")) or "zh-CN",
                top_k=self._reply_top_k,
                max_context_chars=self._reply_max_context_chars,
            )
        except Exception:
            return RagRetrievalResult(context="", references=[])


def _review_score(value: Any) -> int | None:
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _optional_text(value: Any) -> str | None:
    text = str(value).strip() if value is not None else ""
    return text or None
