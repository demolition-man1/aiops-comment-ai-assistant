from types import SimpleNamespace

import pytest

from app.ai.chains.content_generation import ContentGenerationChain
from app.ai.chains.negative_reply import NegativeReplyChain
from app.ai.context import AiInvocationContext
from app.ai.errors import AiJobCancelledError, AiOutputValidationError
from app.ai.provider import LangChainProvider
from app.ai.results import AiInvocationResult


class FakeStreamModel:
    def __init__(self, chunks: list[object]) -> None:
        self._chunks = chunks

    def stream(self, _prompt: object, **_kwargs: object):
        yield from self._chunks


class CancelAfterFirstChunk:
    def __init__(self) -> None:
        self.calls = 0

    def is_cancel_requested(self, _job_id: int | None) -> bool:
        self.calls += 1
        return self.calls > 2


class DeltaPublisher:
    def __init__(self) -> None:
        self.chunks: list[str] = []

    def is_cancel_requested(self, _job_id: int | None) -> bool:
        return False

    def publish_text_delta(self, _context: AiInvocationContext | None, text: str) -> None:
        self.chunks.append(text)


class StreamProvider:
    def __init__(self, content: str) -> None:
        self.content = content
        self.stream_prompts: list[object] = []

    def stream_text(self, prompt: object, **kwargs: object) -> AiInvocationResult[str]:
        self.stream_prompts.append(prompt)
        callback = kwargs["on_chunk"]
        callback(self.content[:5])
        callback(self.content[5:])
        return AiInvocationResult(self.content, "fake", 4, 3, 7, False)


def provider_settings() -> SimpleNamespace:
    return SimpleNamespace(ai_max_retries=0, ai_model="fake")


def test_stream_text_preserves_chunk_order_and_complete_buffer() -> None:
    model = FakeStreamModel([SimpleNamespace(content="hello "), SimpleNamespace(content="world")])
    provider = LangChainProvider(provider_settings(), chat_model=model)
    chunks: list[str] = []

    result = provider.stream_text("prompt", on_chunk=chunks.append)

    assert chunks == ["hello ", "world"]
    assert result.value == "hello world"
    assert result.total_tokens > 0


def test_stream_text_rejects_empty_stream() -> None:
    provider = LangChainProvider(provider_settings(), chat_model=FakeStreamModel([]))

    with pytest.raises(AiOutputValidationError):
        provider.stream_text("prompt", on_chunk=lambda _chunk: None)


def test_stream_text_checks_cancellation_between_chunks() -> None:
    model = FakeStreamModel([SimpleNamespace(content="first"), SimpleNamespace(content="second")])
    provider = LangChainProvider(provider_settings(), chat_model=model)
    context = AiInvocationContext(job_id=7, job_type="negative_reply", progress_publisher=CancelAfterFirstChunk())

    with pytest.raises(AiJobCancelledError):
        provider.stream_text("prompt", context=context, on_chunk=lambda _chunk: None)


def test_text_chains_wrap_streamed_text_in_their_final_schema() -> None:
    publisher = DeltaPublisher()
    context = AiInvocationContext(job_id=9, job_type="negative_reply", progress_publisher=publisher)
    reply = NegativeReplyChain(StreamProvider("Thank you for the feedback.")).generate(
        "review", context=context, stream_text=True
    )
    content_context = AiInvocationContext(job_id=10, job_type="content", progress_publisher=publisher)
    content = ContentGenerationChain(StreamProvider("Durable everyday essential.")).generate(
        "content", context=content_context, stream_text=True
    )

    assert reply.value.reply_content == "Thank you for the feedback."
    assert content.value.generated_content == "Durable everyday essential."
    assert publisher.chunks[0] == "Thank"
