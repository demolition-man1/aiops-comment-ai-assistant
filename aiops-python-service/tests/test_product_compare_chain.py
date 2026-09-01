from app.ai.chains.product_compare import ProductCompareChain
from app.ai.results import AiInvocationResult
from app.ai.schemas import ProductCompareOutput


class FakeProvider:
    def __init__(self) -> None:
        self.prompts: list[object] = []

    def invoke_structured(
        self,
        prompt: object,
        _schema: type[ProductCompareOutput],
    ) -> AiInvocationResult[ProductCompareOutput]:
        self.prompts.append(prompt)
        return AiInvocationResult(
            value=ProductCompareOutput.model_validate(
                {
                    "compareSummary": "The left product has stronger review stability.",
                    "advantageAnalysis": "The left product has fewer delivery complaints.",
                    "riskAnalysis": "The right product needs packaging improvements.",
                    "operationSuggestions": "Improve packaging before increasing promotion.",
                }
            ),
            model_name="deepseek-chat",
            input_tokens=12,
            output_tokens=8,
            total_tokens=20,
            token_usage_estimated=False,
        )


def test_product_compare_chain_returns_the_existing_api_fields() -> None:
    provider = FakeProvider()

    result = ProductCompareChain(provider).generate("Compare the supplied review analyses.")

    assert result.value.compare_summary == "The left product has stronger review stability."
    assert result.value.operation_suggestions == "Improve packaging before increasing promotion."
    assert "json" in "\n".join(message.content for message in provider.prompts[0]).lower()
