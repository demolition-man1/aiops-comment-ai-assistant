from collections.abc import Callable
from typing import Any, Protocol

from langchain_core.messages import HumanMessage, SystemMessage

from app.ai.context import AiInvocationContext
from app.ai.results import AiInvocationResult
from app.ai.schemas import ContentGenerationOutput


class ContentGenerationProvider(Protocol):
    def invoke_structured(
        self,
        prompt: Any,
        schema: type[ContentGenerationOutput],
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[ContentGenerationOutput]: ...

    def stream_text(
        self,
        prompt: Any,
        *,
        context: AiInvocationContext | None = None,
        on_chunk: Callable[[str], None],
    ) -> AiInvocationResult[str]: ...


class ContentGenerationChain:
    _SYSTEM_MESSAGE = (
        "You write practical ecommerce marketing copy. Return exactly one JSON object matching the required schema. "
        "Do not invent product facts, discounts, delivery promises, or store policies."
    )
    _STREAM_MESSAGE = (
        "You write practical ecommerce marketing copy. Return only the final marketing copy text. "
        "Do not return JSON, Markdown wrappers, explanations, or invented product facts, discounts, delivery promises, or store policies."
    )

    def __init__(self, provider: ContentGenerationProvider) -> None:
        self._provider = provider

    def generate(
        self,
        rendered_prompt: str,
        *,
        context: AiInvocationContext | None = None,
        stream_text: bool = False,
    ) -> AiInvocationResult[ContentGenerationOutput]:
        if stream_text:
            return self._stream_content(rendered_prompt, context)
        messages = [SystemMessage(content=self._SYSTEM_MESSAGE), HumanMessage(content=rendered_prompt)]
        if context is None:
            return self._provider.invoke_structured(messages, ContentGenerationOutput)
        return self._provider.invoke_structured(
            messages,
            ContentGenerationOutput,
            context=context,
        )

    def _stream_content(
        self,
        rendered_prompt: str,
        context: AiInvocationContext | None,
    ) -> AiInvocationResult[ContentGenerationOutput]:
        publisher = getattr(context, "progress_publisher", None)
        result = self._provider.stream_text(
            [SystemMessage(content=self._STREAM_MESSAGE), HumanMessage(content=rendered_prompt)],
            context=context,
            on_chunk=lambda text: publisher.publish_text_delta(context, text) if publisher is not None else None,
        )
        output = ContentGenerationOutput(generatedContent=result.value)
        return AiInvocationResult(
            value=output,
            model_name=result.model_name,
            input_tokens=result.input_tokens,
            output_tokens=result.output_tokens,
            total_tokens=result.total_tokens,
            token_usage_estimated=result.token_usage_estimated,
            latency_ms=result.latency_ms,
        )
