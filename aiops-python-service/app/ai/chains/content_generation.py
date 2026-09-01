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


class ContentGenerationChain:
    _SYSTEM_MESSAGE = (
        "You write practical ecommerce marketing copy. Return exactly one JSON object matching the required schema. "
        "Do not invent product facts, discounts, delivery promises, or store policies."
    )

    def __init__(self, provider: ContentGenerationProvider) -> None:
        self._provider = provider

    def generate(
        self,
        rendered_prompt: str,
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[ContentGenerationOutput]:
        messages = [SystemMessage(content=self._SYSTEM_MESSAGE), HumanMessage(content=rendered_prompt)]
        if context is None:
            return self._provider.invoke_structured(messages, ContentGenerationOutput)
        return self._provider.invoke_structured(
            messages,
            ContentGenerationOutput,
            context=context,
        )
