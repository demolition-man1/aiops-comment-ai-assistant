import pytest
from pydantic import ValidationError

from app.ai.chains.comment_analysis import CommentAnalysisChain
from app.ai.errors import AiOutputValidationError
from app.ai.results import AiInvocationResult
from app.ai.schemas import CommentAnalysisOutput


def result_for(payload: dict[str, object]) -> AiInvocationResult[CommentAnalysisOutput]:
    return AiInvocationResult(
        value=CommentAnalysisOutput.model_validate(payload),
        model_name="deepseek-chat",
        input_tokens=12,
        output_tokens=8,
        total_tokens=20,
        token_usage_estimated=False,
    )


class FakeProvider:
    def __init__(
        self,
        structured_result: AiInvocationResult[CommentAnalysisOutput] | None = None,
        structured_error: Exception | None = None,
        text_result: AiInvocationResult[str] | None = None,
    ) -> None:
        self.structured_result = structured_result
        self.structured_error = structured_error
        self.text_result = text_result
        self.structured_prompts: list[object] = []
        self.text_prompts: list[object] = []
        self.text_retry_limits: list[object] = []

    def invoke_structured(
        self,
        prompt: object,
        _schema: type[CommentAnalysisOutput],
    ) -> AiInvocationResult[CommentAnalysisOutput]:
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


def valid_payload() -> dict[str, object]:
    return {
        "sentiment": "negative",
        "sentimentConfidence": 0.93,
        "primaryProblem": "delivery",
        "problems": [
            {
                "type": "delivery",
                "confidence": 0.93,
                "evidence": "arrived late",
            }
        ],
    }


def test_comment_analysis_output_rejects_invalid_classification_contract() -> None:
    invalid_payloads = [
        {**valid_payload(), "sentiment": "mixed"},
        {**valid_payload(), "sentimentConfidence": 1.01},
        {**valid_payload(), "primaryProblem": "quality"},
        {**valid_payload(), "problems": [valid_payload()["problems"][0]] * 6},
    ]

    for payload in invalid_payloads:
        with pytest.raises(ValidationError):
            CommentAnalysisOutput.model_validate(payload)


def test_chain_returns_grounded_analysis_and_keeps_score_separate_from_review() -> None:
    review_text = "The order arrived late and the box was damaged."
    provider = FakeProvider(structured_result=result_for(valid_payload()))
    chain = CommentAnalysisChain(provider)

    result = chain.generate(review_text, 1, "Analyze this customer review.")

    assert result.value.primary_problem == "delivery"
    assert result.value.problems[0].evidence == "arrived late"
    assert len(provider.structured_prompts) == 1
    assert len(provider.text_prompts) == 0
    messages = provider.structured_prompts[0]
    assert isinstance(messages, list)
    assert messages[-1].content == "Review score: 1\nReview text: The order arrived late and the box was damaged."


def test_chain_rejects_analysis_when_evidence_is_not_in_current_review() -> None:
    ungrounded_payload = valid_payload()
    ungrounded_payload["problems"] = [
        {"type": "delivery", "confidence": 0.93, "evidence": "refund was issued"}
    ]
    repair = AiInvocationResult(
        value=(
            '{"sentiment":"negative","sentimentConfidence":0.93,'
            '"primaryProblem":"delivery","problems":['
            '{"type":"delivery","confidence":0.93,"evidence":"refund was issued"}]}'
        ),
        model_name="deepseek-chat",
        input_tokens=6,
        output_tokens=5,
        total_tokens=11,
        token_usage_estimated=False,
    )
    provider = FakeProvider(structured_result=result_for(ungrounded_payload), text_result=repair)

    with pytest.raises(AiOutputValidationError):
        CommentAnalysisChain(provider).generate("The parcel arrived late.", 1, "Analyze this review.")

    assert len(provider.text_prompts) == 1
    assert provider.text_retry_limits == [0]


def test_chain_repairs_one_invalid_structured_response_and_aggregates_usage() -> None:
    repair = AiInvocationResult(
        value=(
            '```json\n'
            '{"sentiment":"negative","sentimentConfidence":0.93,'
            '"primaryProblem":"delivery","problems":['
            '{"type":"delivery","confidence":0.93,"evidence":"arrived late"}]}'
            '\n```'
        ),
        model_name="deepseek-chat",
        input_tokens=8,
        output_tokens=7,
        total_tokens=15,
        token_usage_estimated=False,
    )
    provider = FakeProvider(
        structured_error=AiOutputValidationError(
            "invalid", input_tokens=14, output_tokens=4, total_tokens=18, token_usage_estimated=False
        ),
        text_result=repair,
    )

    result = CommentAnalysisChain(provider).generate(
        "The order arrived late and the box was damaged.", 1, "Analyze this review."
    )

    assert result.value.primary_problem == "delivery"
    assert result.input_tokens == 22
    assert result.output_tokens == 11
    assert result.total_tokens == 33
    assert result.token_usage_estimated is False
    assert provider.text_retry_limits == [0]


def test_chain_rejects_blank_review_without_provider_call() -> None:
    provider = FakeProvider(structured_result=result_for(valid_payload()))

    with pytest.raises(ValueError, match="review_text"):
        CommentAnalysisChain(provider).generate("   ", 1, "Analyze this review.")

    assert provider.structured_prompts == []
