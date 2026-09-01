from typing import Any, Protocol

from langchain_core.messages import HumanMessage, SystemMessage

from app.ai.context import AiInvocationContext
from app.ai.results import AiInvocationResult
from app.ai.schemas import CommentTranslationOutput


class CommentTranslationProvider(Protocol):
    def invoke_structured(
        self,
        prompt: Any,
        schema: type[CommentTranslationOutput],
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[CommentTranslationOutput]: ...


class CommentTranslationChain:
    _SYSTEM_MESSAGE = (
        "You precisely translate ecommerce customer reviews. Return exactly one JSON object matching the required schema. "
        "Preserve facts, sentiment, quantities, and named entities without adding explanations or invented details."
    )

    def __init__(self, provider: CommentTranslationProvider) -> None:
        self._provider = provider

    def generate(
        self,
        rendered_prompt: str,
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[CommentTranslationOutput]:
        messages = [SystemMessage(content=self._SYSTEM_MESSAGE), HumanMessage(content=rendered_prompt)]
        if context is None:
            return self._provider.invoke_structured(messages, CommentTranslationOutput)
        return self._provider.invoke_structured(
            messages,
            CommentTranslationOutput,
            context=context,
        )
