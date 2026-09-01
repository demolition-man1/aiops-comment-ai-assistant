from app.ai.chains.content_generation import ContentGenerationChain
from app.ai.results import AiInvocationResult
from app.ai.schemas import ContentGenerationOutput


class FakeProvider:
    def __init__(self) -> None:
        self.prompts: list[object] = []

    def invoke_structured(
        self,
        prompt: object,
        _schema: type[ContentGenerationOutput],
    ) -> AiInvocationResult[ContentGenerationOutput]:
        self.prompts.append(prompt)
        return AiInvocationResult(
            value=ContentGenerationOutput.model_validate(
                {"generatedContent": "A durable product for everyday use."}
            ),
            model_name="deepseek-chat",
            input_tokens=9,
            output_tokens=7,
            total_tokens=16,
            token_usage_estimated=False,
        )


def test_content_generation_chain_returns_nonempty_generated_content() -> None:
    provider = FakeProvider()

    result = ContentGenerationChain(provider).generate("Write a concise product description.")

    assert result.value.generated_content == "A durable product for everyday use."
    assert "json" in "\n".join(message.content for message in provider.prompts[0]).lower()
