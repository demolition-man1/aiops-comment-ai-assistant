from dataclasses import asdict
from types import SimpleNamespace

import pytest
from pydantic import ValidationError

from app.ai.errors import (
    AiAuthenticationError,
    AiProviderRequestError,
    AiProviderTemporaryError,
    AiProviderTimeoutError,
    AiRateLimitError,
)
from app.ai.provider import LangChainProvider
from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput


class ProviderStatusError(Exception):
    def __init__(self, status_code: int) -> None:
        super().__init__(f"provider status {status_code}")
        self.status_code = status_code


class FakeStructuredRunnable:
    def __init__(self, model: "FakeChatModel") -> None:
        self.model = model

    def invoke(self, _input: object) -> object:
        return self.model.next_value()


class FakeChatModel:
    def __init__(self, values: list[object]) -> None:
        self.values = values
        self.call_count = 0

    def with_structured_output(self, _schema: type[object], **_kwargs: object) -> FakeStructuredRunnable:
        return FakeStructuredRunnable(self)

    def next_value(self) -> object:
        self.call_count += 1
        value = self.values.pop(0)
        if isinstance(value, Exception):
            raise value
        return value

    def invoke(self, _input: object) -> object:
        return self.next_value()


def settings_for_test(ai_max_retries: int = 2) -> SimpleNamespace:
    return SimpleNamespace(
        ai_api_key="",
        ai_base_url="https://api.deepseek.com",
        ai_model="deepseek-chat",
        ai_timeout=30,
        ai_max_retries=ai_max_retries,
    )


def structured_response(reply: str, usage: dict[str, int] | None) -> dict[str, object]:
    raw = SimpleNamespace(usage_metadata=usage, response_metadata={})
    return {
        "parsed": NegativeReplyOutput.model_validate({"replyContent": reply}),
        "raw": raw,
        "parsing_error": None,
    }


def test_negative_reply_output_serializes_existing_reply_content_key() -> None:
    output = NegativeReplyOutput.model_validate({"replyContent": "  Thank you for the feedback.  "})

    assert output.reply_content == "Thank you for the feedback."
    assert output.model_dump(by_alias=True) == {"replyContent": "Thank you for the feedback."}


def test_negative_reply_output_rejects_blank_reply() -> None:
    with pytest.raises(ValidationError):
        NegativeReplyOutput.model_validate({"replyContent": "  "})


def test_invocation_result_keeps_usage_metadata() -> None:
    result = AiInvocationResult(
        value=NegativeReplyOutput.model_validate({"replyContent": "Thank you."}),
        model_name="deepseek-chat",
        input_tokens=12,
        output_tokens=8,
        total_tokens=20,
        token_usage_estimated=False,
    )

    assert asdict(result)["total_tokens"] == 20
    assert result.token_usage_estimated is False


def test_provider_error_exposes_only_safe_message() -> None:
    error = AiProviderTemporaryError("AI provider is temporarily unavailable")

    assert error.public_message == "AI provider is temporarily unavailable"


def test_provider_prefers_usage_metadata_from_structured_model() -> None:
    model = FakeChatModel(
        [structured_response("Thank you for the feedback.", {"input_tokens": 12, "output_tokens": 8, "total_tokens": 20})]
    )
    provider = LangChainProvider(settings_for_test(), chat_model=model, sleep=lambda _seconds: None)

    result = provider.invoke_structured("prompt", NegativeReplyOutput)

    assert result.total_tokens == 20
    assert result.input_tokens == 12
    assert result.output_tokens == 8
    assert result.token_usage_estimated is False


def test_provider_retries_only_temporary_failures() -> None:
    model = FakeChatModel([ProviderStatusError(503), ProviderStatusError(503), ProviderStatusError(503)])
    provider = LangChainProvider(settings_for_test(ai_max_retries=2), chat_model=model, sleep=lambda _seconds: None)

    with pytest.raises(AiProviderTemporaryError):
        provider.invoke_structured("prompt", NegativeReplyOutput)

    assert model.call_count == 3


@pytest.mark.parametrize(
    ("provider_error", "expected_error"),
    [
        (TimeoutError("request timed out"), AiProviderTimeoutError),
        (ConnectionError("connection dropped"), AiProviderTemporaryError),
        (ProviderStatusError(429), AiRateLimitError),
        (ProviderStatusError(502), AiProviderTemporaryError),
    ],
)
def test_provider_retries_each_supported_temporary_failure(
    provider_error: Exception,
    expected_error: type[Exception],
) -> None:
    model = FakeChatModel([provider_error, provider_error])
    provider = LangChainProvider(settings_for_test(ai_max_retries=1), chat_model=model, sleep=lambda _seconds: None)

    with pytest.raises(expected_error):
        provider.invoke_structured("prompt", NegativeReplyOutput)

    assert model.call_count == 2


def test_provider_does_not_retry_authentication_failure() -> None:
    model = FakeChatModel([ProviderStatusError(401)])
    provider = LangChainProvider(settings_for_test(ai_max_retries=2), chat_model=model, sleep=lambda _seconds: None)

    with pytest.raises(AiAuthenticationError):
        provider.invoke_structured("prompt", NegativeReplyOutput)

    assert model.call_count == 1


def test_provider_does_not_retry_non_temporary_request_failure() -> None:
    model = FakeChatModel([ProviderStatusError(400)])
    provider = LangChainProvider(settings_for_test(ai_max_retries=2), chat_model=model, sleep=lambda _seconds: None)

    with pytest.raises(AiProviderRequestError):
        provider.invoke_structured("prompt", NegativeReplyOutput)

    assert model.call_count == 1


def test_provider_honors_zero_retry_override_for_repair_calls() -> None:
    model = FakeChatModel([ProviderStatusError(503)])
    provider = LangChainProvider(settings_for_test(ai_max_retries=2), chat_model=model, sleep=lambda _seconds: None)

    with pytest.raises(AiProviderTemporaryError):
        provider.invoke_text("prompt", max_retries=0)

    assert model.call_count == 1


def test_provider_marks_character_estimate_when_usage_is_missing() -> None:
    model = FakeChatModel([structured_response("A reply without usage metadata.", None)])
    provider = LangChainProvider(settings_for_test(), chat_model=model, sleep=lambda _seconds: None)

    result = provider.invoke_structured("prompt", NegativeReplyOutput)

    assert result.total_tokens > 0
    assert result.token_usage_estimated is True


def test_provider_returns_text_with_usage_metadata() -> None:
    response = SimpleNamespace(
        content="Please contact our support team.",
        usage_metadata={"input_tokens": 10, "output_tokens": 6, "total_tokens": 16},
        response_metadata={},
    )
    provider = LangChainProvider(settings_for_test(), chat_model=FakeChatModel([response]), sleep=lambda _seconds: None)

    result = provider.invoke_text("prompt")

    assert result.value == "Please contact our support team."
    assert result.total_tokens == 16
    assert result.token_usage_estimated is False
