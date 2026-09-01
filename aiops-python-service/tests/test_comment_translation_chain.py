from app.ai.chains.comment_translation import CommentTranslationChain
from app.ai.results import AiInvocationResult
from app.ai.schemas import CommentTranslationOutput


class FakeProvider:
    def __init__(self) -> None:
        self.prompts: list[object] = []

    def invoke_structured(
        self,
        prompt: object,
        _schema: type[CommentTranslationOutput],
    ) -> AiInvocationResult[CommentTranslationOutput]:
        self.prompts.append(prompt)
        return AiInvocationResult(
            value=CommentTranslationOutput.model_validate(
                {"translatedContent": "The product arrived scratched.", "sourceLanguage": "pt-BR"}
            ),
            model_name="deepseek-chat",
            input_tokens=11,
            output_tokens=8,
            total_tokens=19,
            token_usage_estimated=False,
        )


def test_comment_translation_chain_preserves_translation_contract() -> None:
    provider = FakeProvider()

    result = CommentTranslationChain(provider).generate("Translate this review into English.")

    assert result.value.translated_content == "The product arrived scratched."
    assert result.value.source_language == "pt-BR"
    assert "json" in "\n".join(message.content for message in provider.prompts[0]).lower()
