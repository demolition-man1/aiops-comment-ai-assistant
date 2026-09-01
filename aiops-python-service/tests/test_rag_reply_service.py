import pytest

from app.ai.errors import AiProviderTimeoutError
from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput
from app.rag.models import RagReference, RagRetrievalResult
from app.rag.reply_service import RagReplyService


class FakeRetriever:
    def __init__(self, result=None, error: Exception | None = None) -> None:
        self.result = result or RagRetrievalResult(context="", references=[])
        self.error = error
        self.calls: list[dict[str, object]] = []

    def retrieve(self, **kwargs):
        self.calls.append(kwargs)
        if self.error is not None:
            raise self.error
        return self.result


class FakeChain:
    def __init__(self, error: Exception | None = None) -> None:
        self.reference_contexts: list[str | None] = []
        self.error = error

    def generate(self, _prompt: str, *, reference_context: str | None = None) -> AiInvocationResult[NegativeReplyOutput]:
        self.reference_contexts.append(reference_context)
        if self.error is not None:
            raise self.error
        return AiInvocationResult(
            value=NegativeReplyOutput.model_validate({"replyContent": "We will check the delivery status."}),
            model_name="deepseek-chat",
            input_tokens=10,
            output_tokens=8,
            total_tokens=18,
            token_usage_estimated=False,
        )


def _request() -> dict[str, object]:
    return {
        "commentContent": "The delivery was late.",
        "reviewScore": 1,
        "problemType": "logistics",
        "language": "en-US",
    }


def test_reply_service_passes_retrieved_context_and_returns_programmatic_references() -> None:
    reference = RagReference("problem_solution", 14, "Delivery guide", 0.91)
    chain = FakeChain()
    retriever = FakeRetriever(RagRetrievalResult(context="Delivery guidance", references=[reference]))
    service = RagReplyService(
        retriever=retriever,
        reply_chain=chain,
        reply_top_k=2,
        reply_max_context_chars=1800,
    )

    result = service.generate(request=_request(), rendered_prompt="reply prompt")

    assert result.rag_used is True
    assert result.references == [reference]
    assert chain.reference_contexts == ["Delivery guidance"]
    assert result.invocation.total_tokens == 18
    assert retriever.calls == [
        {
            "review_text": "The delivery was late.",
            "review_score": 1,
            "problem_type": "logistics",
            "language": "en-US",
            "top_k": 2,
            "max_context_chars": 1800,
        }
    ]


def test_reply_service_falls_back_when_retrieval_fails_but_preserves_provider_errors() -> None:
    chain = FakeChain()
    service = RagReplyService(retriever=FakeRetriever(error=RuntimeError("chroma unavailable")), reply_chain=chain)

    result = service.generate(request=_request(), rendered_prompt="reply prompt")

    assert result.rag_used is False
    assert result.references == []
    assert chain.reference_contexts == [None]

    failing = RagReplyService(
        retriever=FakeRetriever(),
        reply_chain=FakeChain(error=AiProviderTimeoutError("AI provider request timed out")),
    )
    with pytest.raises(AiProviderTimeoutError):
        failing.generate(request=_request(), rendered_prompt="reply prompt")
