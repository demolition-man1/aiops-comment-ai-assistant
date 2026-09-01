import json
from typing import Any, Protocol

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import ValidationError

from app.ai.context import AiInvocationContext
from app.ai.errors import AiOutputValidationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import CommentAnalysisOutput


class CommentAnalysisProvider(Protocol):
    def invoke_structured(
        self,
        prompt: Any,
        schema: type[CommentAnalysisOutput],
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[CommentAnalysisOutput]: ...

    def invoke_text(
        self,
        prompt: Any,
        *,
        max_retries: int | None = None,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[str]: ...


class CommentAnalysisChain:
    _SYSTEM_MESSAGE = (
        "You classify ecommerce reviews using only the current review text and score. "
        "Return the requested structured output. Each evidence value must be a direct excerpt from the current review. "
        "Do not infer actions, order facts, or customer details that are not present."
    )
    _REPAIR_MESSAGE = (
        "Regenerate the analysis as exactly one JSON object matching the requested schema. "
        "Use only evidence excerpts that appear in the current review. Do not include Markdown or explanations."
    )

    def __init__(self, provider: CommentAnalysisProvider) -> None:
        self._provider = provider

    def generate(
        self,
        review_text: str,
        review_score: int,
        rendered_prompt: str,
        *,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[CommentAnalysisOutput]:
        if not isinstance(review_text, str) or not review_text.strip():
            raise ValueError("review_text must not be blank")

        messages = self._messages(review_text, review_score, rendered_prompt)
        try:
            result = self._invoke_structured(messages, context)
            self._validate_evidence(result.value, review_text)
            return result
        except AiOutputValidationError as initial_error:
            repaired = self._invoke_text(
                messages + [HumanMessage(content=self._REPAIR_MESSAGE)],
                context,
            )
            output = self.parse_output(repaired.value)
            self._validate_evidence(output, review_text)
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

    def _invoke_structured(
        self,
        messages: list[SystemMessage | HumanMessage],
        context: AiInvocationContext | None,
    ) -> AiInvocationResult[CommentAnalysisOutput]:
        if context is None:
            return self._provider.invoke_structured(messages, CommentAnalysisOutput)
        return self._provider.invoke_structured(messages, CommentAnalysisOutput, context=context)

    def _invoke_text(
        self,
        messages: list[SystemMessage | HumanMessage],
        context: AiInvocationContext | None,
    ) -> AiInvocationResult[str]:
        if context is None:
            return self._provider.invoke_text(messages, max_retries=0)
        return self._provider.invoke_text(messages, max_retries=0, context=context)

    @classmethod
    def parse_output(cls, content: str) -> CommentAnalysisOutput:
        text = content.strip()
        if text.startswith("```"):
            lines = text.splitlines()
            if len(lines) < 3 or not lines[-1].strip().startswith("```"):
                raise AiOutputValidationError("AI provider returned an invalid structured response")
            text = "\n".join(lines[1:-1]).strip()
        try:
            return CommentAnalysisOutput.model_validate(json.loads(text))
        except (json.JSONDecodeError, ValidationError, TypeError) as exception:
            raise AiOutputValidationError("AI provider returned an invalid structured response") from exception

    @classmethod
    def _messages(
        cls,
        review_text: str,
        review_score: int,
        rendered_prompt: str,
    ) -> list[SystemMessage | HumanMessage]:
        return [
            SystemMessage(content=cls._SYSTEM_MESSAGE),
            HumanMessage(content=rendered_prompt),
            HumanMessage(content=f"Review score: {review_score}\nReview text: {review_text.strip()}"),
        ]

    @staticmethod
    def _validate_evidence(output: CommentAnalysisOutput, review_text: str) -> None:
        normalized_review = CommentAnalysisChain._normalize(review_text)
        for problem in output.problems:
            if CommentAnalysisChain._normalize(problem.evidence) not in normalized_review:
                raise AiOutputValidationError("AI provider returned evidence outside the current review")

    @staticmethod
    def _normalize(value: str) -> str:
        return " ".join(value.casefold().split())

    @staticmethod
    def _combined_tokens(initial: int | None, repaired: int | None, initial_has_usage: bool) -> int | None:
        if not initial_has_usage:
            return repaired
        if initial is None or repaired is None:
            return None
        return initial + repaired
