import json
import random
import time
from collections.abc import Callable
from typing import Any, TypeVar

import httpx
from langchain_deepseek import ChatDeepSeek
from pydantic import BaseModel, ValidationError

from app.ai.context import AiInvocationContext
from app.ai.errors import (
    AiAuthenticationError,
    AiConfigurationError,
    AiJobCancelledError,
    AiOutputValidationError,
    AiProviderRequestError,
    AiProviderTemporaryError,
    AiProviderTimeoutError,
    AiRateLimitError,
    AiServiceError,
)
from app.ai.results import AiInvocationResult
from app.config import Settings, settings


T = TypeVar("T", bound=BaseModel)


class LangChainProvider:
    def __init__(
        self,
        provider_settings: Settings | Any = settings,
        chat_model: Any | None = None,
        model_options: dict[str, Any] | None = None,
        sleep: Callable[[float], None] = time.sleep,
        jitter: Callable[[float, float], float] = random.uniform,
    ) -> None:
        self._settings = provider_settings
        self._chat_model = chat_model
        self._model_options = dict(model_options or {})
        self._sleep = sleep
        self._jitter = jitter

    def invoke_structured(
        self,
        prompt: Any,
        schema: type[T],
        *,
        max_retries: int | None = None,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[T]:
        runnable = self._model().with_structured_output(schema, method="json_mode", include_raw=True)
        started_at = time.monotonic_ns()
        response = self._invoke_with_retry(
            lambda: self._invoke_runnable(runnable, prompt, context),
            max_retries=max_retries,
        )
        latency_ms = self._elapsed_ms(started_at)
        try:
            value, raw = self._structured_value(response, schema)
        except AiOutputValidationError as exception:
            raw = response.get("raw") if isinstance(response, dict) else None
            raw_content = getattr(raw, "content", "")
            input_tokens, output_tokens, total_tokens, estimated = self._token_usage(prompt, raw_content, raw)
            raise AiOutputValidationError(
                exception.public_message,
                input_tokens=input_tokens,
                output_tokens=output_tokens,
                total_tokens=total_tokens,
                token_usage_estimated=estimated,
                latency_ms=latency_ms,
            ) from exception
        input_tokens, output_tokens, total_tokens, estimated = self._token_usage(prompt, value, raw)
        return AiInvocationResult(
            value=value,
            model_name=self._model_name(raw),
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            total_tokens=total_tokens,
            token_usage_estimated=estimated,
            latency_ms=latency_ms,
        )

    def invoke_text(
        self,
        prompt: Any,
        *,
        max_retries: int | None = None,
        context: AiInvocationContext | None = None,
    ) -> AiInvocationResult[str]:
        started_at = time.monotonic_ns()
        raw = self._invoke_with_retry(
            lambda: self._invoke_runnable(self._model(), prompt, context),
            max_retries=max_retries,
        )
        latency_ms = self._elapsed_ms(started_at)
        content = self._content_text(getattr(raw, "content", raw))
        if not content:
            raise AiOutputValidationError("AI provider returned an empty response")
        input_tokens, output_tokens, total_tokens, estimated = self._token_usage(prompt, content, raw)
        return AiInvocationResult(
            value=content,
            model_name=self._model_name(raw),
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            total_tokens=total_tokens,
            token_usage_estimated=estimated,
            latency_ms=latency_ms,
        )

    def _model(self) -> Any:
        if self._chat_model is not None:
            return self._chat_model
        if not self._settings.ai_api_key:
            raise AiConfigurationError("AI_API_KEY is not configured")
        self._chat_model = ChatDeepSeek(
            model=self._settings.ai_model,
            api_key=self._settings.ai_api_key,
            base_url=self._settings.ai_base_url,
            timeout=self._settings.ai_timeout,
            max_retries=0,
            **self._model_options,
        )
        return self._chat_model

    def _invoke_with_retry(self, action: Callable[[], Any], *, max_retries: int | None = None) -> Any:
        retries = self._settings.ai_max_retries if max_retries is None else max_retries
        if retries < 0:
            raise ValueError("max_retries cannot be negative")
        attempts = retries + 1
        for attempt in range(attempts):
            try:
                return action()
            except Exception as exception:
                error = self._classify_error(exception)
                if not self._is_retryable(error) or attempt == attempts - 1:
                    raise error from exception
                delay = min(0.5 * (2**attempt), 2.0) + self._jitter(0.0, 0.25)
                self._sleep(delay)
        raise AiProviderTemporaryError("AI provider is temporarily unavailable")

    @staticmethod
    def _invoke_runnable(runnable: Any, prompt: Any, context: AiInvocationContext | None) -> Any:
        LangChainProvider._ensure_not_cancelled(context)
        config = LangChainProvider._runnable_config(context)
        response = runnable.invoke(prompt) if config is None else runnable.invoke(prompt, config=config)
        LangChainProvider._ensure_not_cancelled(context)
        return response

    @staticmethod
    def _ensure_not_cancelled(context: AiInvocationContext | None) -> None:
        publisher = getattr(context, "progress_publisher", None)
        if context is not None and publisher is not None and publisher.is_cancel_requested(context.job_id):
            raise AiJobCancelledError("AI job was cancelled")

    @staticmethod
    def _runnable_config(context: AiInvocationContext | None) -> dict[str, Any] | None:
        if context is None:
            return None
        metadata: dict[str, Any] = {"jobType": context.job_type}
        if context.job_id is not None:
            metadata["jobId"] = context.job_id
        if context.target_reference:
            metadata["targetReference"] = context.target_reference
        config: dict[str, Any] = {"metadata": metadata}
        if context.callbacks:
            config["callbacks"] = list(context.callbacks)
        return config

    @staticmethod
    def _elapsed_ms(started_at: int) -> int:
        return max(0, (time.monotonic_ns() - started_at) // 1_000_000)

    def _structured_value(self, response: Any, schema: type[T]) -> tuple[T, Any]:
        if isinstance(response, dict):
            parsing_error = response.get("parsing_error")
            if parsing_error is not None:
                raise AiOutputValidationError("AI provider returned an invalid structured response")
            raw = response.get("raw")
            value = response.get("parsed")
        else:
            raw = None
            value = response
        try:
            if isinstance(value, schema):
                return value, raw
            return schema.model_validate(value), raw
        except ValidationError as exception:
            raise AiOutputValidationError("AI provider returned an invalid structured response") from exception

    def _token_usage(self, prompt: Any, value: Any, raw: Any) -> tuple[int | None, int | None, int, bool]:
        usage = self._usage_metadata(raw)
        input_tokens = self._int_usage(usage, "input_tokens", "prompt_tokens")
        output_tokens = self._int_usage(usage, "output_tokens", "completion_tokens")
        total_tokens = self._int_usage(usage, "total_tokens")
        if total_tokens is None and input_tokens is not None and output_tokens is not None:
            total_tokens = input_tokens + output_tokens
        if total_tokens is not None:
            return input_tokens, output_tokens, total_tokens, False

        content = self._content_text(value)
        estimate = max(1, (len(self._prompt_text(prompt)) + len(content)) // 4)
        return None, None, estimate, True

    def _usage_metadata(self, raw: Any) -> dict[str, Any]:
        if raw is None:
            return {}
        usage = getattr(raw, "usage_metadata", None)
        if isinstance(usage, dict):
            return usage
        response_metadata = getattr(raw, "response_metadata", None)
        if isinstance(response_metadata, dict):
            token_usage = response_metadata.get("token_usage") or response_metadata.get("usage")
            if isinstance(token_usage, dict):
                return token_usage
        return {}

    def _model_name(self, raw: Any) -> str:
        response_metadata = getattr(raw, "response_metadata", None)
        if isinstance(response_metadata, dict):
            model_name = response_metadata.get("model_name") or response_metadata.get("model")
            if isinstance(model_name, str) and model_name.strip():
                return model_name.strip()
        return self._settings.ai_model

    def _classify_error(self, exception: Exception) -> AiServiceError:
        if isinstance(exception, AiServiceError):
            return exception
        if isinstance(exception, (TimeoutError, httpx.TimeoutException)):
            return AiProviderTimeoutError("AI provider request timed out")
        if isinstance(exception, (ConnectionError, httpx.NetworkError)):
            return AiProviderTemporaryError("AI provider is temporarily unavailable")

        status_code = getattr(exception, "status_code", None)
        if status_code in {401, 403}:
            return AiAuthenticationError("AI provider authentication failed")
        if status_code == 429:
            return AiRateLimitError("AI provider rate limit exceeded")
        if isinstance(status_code, int) and status_code >= 500:
            return AiProviderTemporaryError("AI provider is temporarily unavailable")
        if isinstance(status_code, int) and 400 <= status_code < 500:
            return AiProviderRequestError("AI provider rejected the request")
        return AiProviderTemporaryError("AI provider request failed")

    @staticmethod
    def _is_retryable(error: AiServiceError) -> bool:
        return isinstance(error, (AiProviderTimeoutError, AiProviderTemporaryError, AiRateLimitError))

    @staticmethod
    def _int_usage(usage: dict[str, Any], *keys: str) -> int | None:
        for key in keys:
            value = usage.get(key)
            if isinstance(value, int) and value >= 0:
                return value
        return None

    @staticmethod
    def _prompt_text(prompt: Any) -> str:
        if isinstance(prompt, str):
            return prompt
        if isinstance(prompt, list):
            return "\n".join(str(item) for item in prompt)
        if isinstance(prompt, dict):
            return json.dumps(prompt, ensure_ascii=False, default=str)
        return str(prompt)

    @staticmethod
    def _content_text(value: Any) -> str:
        if isinstance(value, BaseModel):
            return value.model_dump_json(by_alias=True)
        if isinstance(value, str):
            return value.strip()
        if isinstance(value, (dict, list)):
            return json.dumps(value, ensure_ascii=False, default=str)
        return str(value).strip()
