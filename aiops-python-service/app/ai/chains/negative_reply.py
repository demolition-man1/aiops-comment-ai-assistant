import json
from collections.abc import Callable
from typing import Any, Protocol

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import ValidationError

from app.ai.context import AiInvocationContext
from app.ai.errors import AiOutputValidationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput


class NegativeReplyProvider(Protocol):
    def invoke_structured(
        self,
        prompt: Any,
        schema: type[NegativeReplyOutput],
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[NegativeReplyOutput]: ...

    def invoke_text(
        self,
        prompt: Any,
        *,
        max_retries: int | None = None,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[str]: ...

    def stream_text(
        self,
        prompt: Any,
        *,
        context: AiInvocationContext | None = None,
        on_chunk: Callable[[str], None],
    ) -> AiInvocationResult[str]: ...


class NegativeReplyChain:
    _SYSTEM_MESSAGE = (
        "You are an ecommerce customer-service supervisor. Use only facts in the current review. "
        "Do not claim refunds, compensation, logistics actions, order checks, or completed actions that were not provided. "
        "Return exactly one JSON object that follows the requested output schema, with a concise, sincere, professional reply."
    )
    _REPAIR_MESSAGE = (
        "Regenerate the reply as exactly one JSON object with the required replyContent field. "
        "Do not include Markdown, explanations, or any additional JSON object."
    )
    _STREAM_MESSAGE = (
        "You are an ecommerce customer-service supervisor. Use only facts in the current review. "
        "Do not claim refunds, compensation, logistics actions, order checks, or completed actions that were not provided. "
        "Return only the final concise, sincere, professional reply text. Do not return JSON, Markdown, or explanations."
    )

    def __init__(self, provider: NegativeReplyProvider) -> None:
        self._provider = provider

    def generate(
        self,
        rendered_prompt: str,
        *,
        reference_context: str | None = None,
        context: AiInvocationContext | None = None,
        stream_text: bool = False,
    ) -> AiInvocationResult[NegativeReplyOutput]:
        if stream_text:
            return self._stream_reply(rendered_prompt, reference_context, context)
        messages = self._messages(rendered_prompt, reference_context=reference_context)
        try:
            return self._invoke_structured(messages, context)
        except AiOutputValidationError as initial_error:
            repaired = self._invoke_text(
                messages + [HumanMessage(content=self._REPAIR_MESSAGE)],
                context,
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
                latency_ms=initial_error.latency_ms + repaired.latency_ms,
            )

    def _stream_reply(
        self,
        rendered_prompt: str,
        reference_context: str | None,
        context: AiInvocationContext | None,
    ) -> AiInvocationResult[NegativeReplyOutput]:
        messages = self._messages(rendered_prompt, reference_context=reference_context, system_message=self._STREAM_MESSAGE)
        publisher = getattr(context, "progress_publisher", None)
        result = self._provider.stream_text(
            messages,
            context=context,
            on_chunk=lambda text: publisher.publish_text_delta(context, text) if publisher is not None else None,
        )
        try:
            output = NegativeReplyOutput(replyContent=result.value)
        except ValidationError as exception:
            raise AiOutputValidationError("AI provider returned an invalid text response") from exception
        return AiInvocationResult(
            value=output,
            model_name=result.model_name,
            input_tokens=result.input_tokens,
            output_tokens=result.output_tokens,
            total_tokens=result.total_tokens,
            token_usage_estimated=result.token_usage_estimated,
            latency_ms=result.latency_ms,
        )

    def _invoke_structured(
        self,
        messages: list[SystemMessage | HumanMessage],
        context: AiInvocationContext | None,
    ) -> AiInvocationResult[NegativeReplyOutput]:
        if context is None:
            return self._provider.invoke_structured(messages, NegativeReplyOutput)
        return self._provider.invoke_structured(messages, NegativeReplyOutput, context=context)

    def _invoke_text(
        self,
        messages: list[SystemMessage | HumanMessage],
        context: AiInvocationContext | None,
    ) -> AiInvocationResult[str]:
        if context is None:
            return self._provider.invoke_text(messages, max_retries=0)
        return self._provider.invoke_text(messages, max_retries=0, context=context)

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
    def _messages(
        cls,
        rendered_prompt: str,
        *,
        reference_context: str | None = None,
        system_message: str | None = None,
    ) -> list[SystemMessage | HumanMessage]:
        messages: list[SystemMessage | HumanMessage] = [SystemMessage(content=system_message or cls._SYSTEM_MESSAGE)]
        if reference_context:
            messages.append(
                SystemMessage(
                    content=(
                        "The following retrieved material is operating guidance, not facts about the current order. "
                        "Use it only when consistent with the current review. Do not claim an action was completed.\n\n"
                        f"{reference_context}"
                    )
                )
            )
        messages.append(HumanMessage(content=rendered_prompt))
        return messages

    @staticmethod
    def _combined_tokens(initial: int | None, repaired: int | None, initial_has_usage: bool) -> int | None:
        if not initial_has_usage:
            return repaired
        if initial is None or repaired is None:
            return None
        return initial + repaired
