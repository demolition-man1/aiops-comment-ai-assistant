import json
from typing import Any, Protocol

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import ValidationError

from app.ai.errors import AiOutputValidationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput


class NegativeReplyProvider(Protocol):
    def invoke_structured(
        self,
        prompt: Any,
        schema: type[NegativeReplyOutput],
    ) -> AiInvocationResult[NegativeReplyOutput]: ...

    def invoke_text(self, prompt: Any, *, max_retries: int | None = None) -> AiInvocationResult[str]: ...


class NegativeReplyChain:
    _SYSTEM_MESSAGE = (
        "You are an ecommerce customer-service supervisor. Use only facts in the current review. "
        "Do not claim refunds, compensation, logistics actions, order checks, or completed actions that were not provided. "
        "Return a concise, sincere, professional reply that follows the requested output schema."
    )
    _REPAIR_MESSAGE = (
        "Regenerate the reply as exactly one JSON object with the required replyContent field. "
        "Do not include Markdown, explanations, or any additional JSON object."
    )

    def __init__(self, provider: NegativeReplyProvider) -> None:
        self._provider = provider

    def generate(self, rendered_prompt: str) -> AiInvocationResult[NegativeReplyOutput]:
        messages = self._messages(rendered_prompt)
        try:
            return self._provider.invoke_structured(messages, NegativeReplyOutput)
        except AiOutputValidationError as initial_error:
            repaired = self._provider.invoke_text(
                messages + [HumanMessage(content=self._REPAIR_MESSAGE)],
                max_retries=0,
            )
            output = self.parse_output(repaired.value)
            initial_has_usage = initial_error.total_tokens > 0
            return AiInvocationResult(
                value=output,
                model_name=repaired.model_name,
                input_tokens=self._combined_tokens(
                    initial_error.input_tokens,
                    repaired.input_tokens,
                    initial_has_usage,
                ),
                output_tokens=self._combined_tokens(
                    initial_error.output_tokens,
                    repaired.output_tokens,
                    initial_has_usage,
                ),
                total_tokens=initial_error.total_tokens + repaired.total_tokens,
                token_usage_estimated=(
                    repaired.token_usage_estimated
                    or (initial_has_usage and initial_error.token_usage_estimated)
                ),
            )

    @classmethod
    def parse_output(cls, content: str) -> NegativeReplyOutput:
        text = content.strip()
        if text.startswith("```"):
            lines = text.splitlines()
            if len(lines) < 3 or not lines[-1].strip().startswith("```"):
                raise AiOutputValidationError("AI provider returned an invalid structured response")
            text = "\n".join(lines[1:-1]).strip()
        try:
            value = json.loads(text)
            return NegativeReplyOutput.model_validate(value)
        except (json.JSONDecodeError, ValidationError, TypeError) as exception:
            raise AiOutputValidationError("AI provider returned an invalid structured response") from exception

    @classmethod
    def _messages(cls, rendered_prompt: str) -> list[SystemMessage | HumanMessage]:
        return [
            SystemMessage(content=cls._SYSTEM_MESSAGE),
            HumanMessage(content=rendered_prompt),
        ]

    @staticmethod
    def _combined_tokens(initial: int | None, repaired: int | None, initial_has_usage: bool) -> int | None:
        if not initial_has_usage:
            return repaired
        if initial is None or repaired is None:
            return None
        return initial + repaired
