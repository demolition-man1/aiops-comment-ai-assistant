import pytest

from app.ai.chains.negative_reply import NegativeReplyChain
from app.ai.errors import AiOutputValidationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput


def result_for(reply_content: str) -> AiInvocationResult[NegativeReplyOutput]:
    return AiInvocationResult(
        value=NegativeReplyOutput.model_validate({"replyContent": reply_content}),
        model_name="deepseek-chat",
        input_tokens=10,
        output_tokens=8,
        total_tokens=18,
        token_usage_estimated=False,
    )


class FakeProvider:
    def __init__(
        self,
        structured_result: AiInvocationResult[NegativeReplyOutput] | None = None,
        structured_error: Exception | None = None,
        text_result: AiInvocationResult[str] | None = None,
    ) -> None:
        self.structured_result = structured_result
        self.structured_error = structured_error
        self.text_result = text_result
        self.structured_prompts: list[object] = []
        self.text_prompts: list[object] = []
        self.text_retry_limits: list[object] = []

    def invoke_structured(self, prompt: object, _schema: type[NegativeReplyOutput]) -> AiInvocationResult[NegativeReplyOutput]:
        self.structured_prompts.append(prompt)
        if self.structured_error is not None:
            raise self.structured_error
        assert self.structured_result is not None
        return self.structured_result

    def invoke_text(self, prompt: object, **kwargs: object) -> AiInvocationResult[str]:
        self.text_prompts.append(prompt)
        self.text_retry_limits.append(kwargs.get("max_retries"))
        assert self.text_result is not None
        return self.text_result


def test_negative_reply_chain_returns_validated_reply_content() -> None:
    provider = FakeProvider(structured_result=result_for("We will verify the delivery issue."))
    chain = NegativeReplyChain(provider)

    result = chain.generate("customer review prompt")

    assert result.value.reply_content == "We will verify the delivery issue."
    assert len(provider.structured_prompts) == 1
    assert len(provider.text_prompts) == 0


def test_negative_reply_chain_includes_json_instruction_for_structured_provider() -> None:
    provider = FakeProvider(structured_result=result_for("We will verify the delivery issue."))
    chain = NegativeReplyChain(provider)

    chain.generate("customer review prompt")

    messages = provider.structured_prompts[0]
    assert "json" in "\n".join(message.content for message in messages).lower()


def test_negative_reply_chain_adds_operating_guidance_as_a_separate_system_message() -> None:
    provider = FakeProvider(structured_result=result_for("We will verify the delivery issue."))
    chain = NegativeReplyChain(provider)

    chain.generate("customer review prompt", reference_context="Source: problem_solution #14 - Delivery guide")

    messages = provider.structured_prompts[0]
    assert len(messages) == 3
    assert "operating guidance, not facts about the current order" in messages[1].content
    assert "problem_solution #14" in messages[1].content
    assert messages[2].content == "customer review prompt"


@pytest.mark.parametrize(
    "content",
    [
        "```json\n{\"replyContent\": \"Thank you.\"}\n```",
        '{"replyContent": "Thank you."}',
    ],
)
def test_negative_reply_chain_accepts_one_complete_json_object(content: str) -> None:
    assert NegativeReplyChain.parse_output(content).reply_content == "Thank you."


@pytest.mark.parametrize(
    "content",
    [
        '{"replyContent":"first"}{"replyContent":"second"}',
        "Thank you for your feedback.",
        '{"replyContent": ""}',
    ],
)
def test_negative_reply_chain_rejects_ambiguous_or_invalid_output(content: str) -> None:
    with pytest.raises(AiOutputValidationError):
        NegativeReplyChain.parse_output(content)


def test_negative_reply_chain_repairs_one_invalid_structured_result() -> None:
    text_result = AiInvocationResult(
        value='```json\n{"replyContent":"Please contact our support team."}\n```',
        model_name="deepseek-chat",
        input_tokens=11,
        output_tokens=9,
        total_tokens=20,
        token_usage_estimated=False,
    )
    provider = FakeProvider(
        structured_error=AiOutputValidationError(
            "invalid",
            input_tokens=13,
            output_tokens=4,
            total_tokens=17,
            token_usage_estimated=False,
        ),
        text_result=text_result,
    )
    chain = NegativeReplyChain(provider)

    result = chain.generate("customer review prompt")

    assert result.value.reply_content == "Please contact our support team."
    assert result.input_tokens == 24
    assert result.output_tokens == 13
    assert result.total_tokens == 37
    assert result.token_usage_estimated is False
    assert len(provider.text_prompts) == 1
    assert provider.text_retry_limits == [0]
