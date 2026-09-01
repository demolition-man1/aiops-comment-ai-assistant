from typing import Any, Protocol

from langchain_core.messages import HumanMessage, SystemMessage

from app.ai.context import AiInvocationContext
from app.ai.results import AiInvocationResult
from app.ai.schemas import ProductCompareOutput


class ProductCompareProvider(Protocol):
    def invoke_structured(
        self,
        prompt: Any,
        schema: type[ProductCompareOutput],
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[ProductCompareOutput]: ...


class ProductCompareChain:
    _SYSTEM_MESSAGE = (
        "You compare ecommerce review analyses using only the supplied evidence. "
        "Return exactly one JSON object matching the required schema. "
        "Do not invent product facts or claim actions have already been completed."
    )

    def __init__(self, provider: ProductCompareProvider) -> None:
        self._provider = provider

    def generate(
        self,
        rendered_prompt: str,
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[ProductCompareOutput]:
        messages = [SystemMessage(content=self._SYSTEM_MESSAGE), HumanMessage(content=rendered_prompt)]
        if context is None:
            return self._provider.invoke_structured(messages, ProductCompareOutput)
        return self._provider.invoke_structured(
            messages,
            ProductCompareOutput,
            context=context,
        )
