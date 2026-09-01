from types import SimpleNamespace

from app.ai.context import AiInvocationContext
from app.ai.provider import LangChainProvider
from app.ai.schemas import NegativeReplyOutput


class RecordingRunnable:
    def __init__(self) -> None:
        self.config: object | None = None

    def invoke(self, _prompt: object, config: object | None = None) -> object:
        self.config = config
        return {
            "parsed": NegativeReplyOutput.model_validate({"replyContent": "Thank you."}),
            "raw": SimpleNamespace(
                usage_metadata={"input_tokens": 5, "output_tokens": 3, "total_tokens": 8},
                response_metadata={},
            ),
            "parsing_error": None,
        }


class RecordingModel:
    def __init__(self, runnable: RecordingRunnable) -> None:
        self.runnable = runnable

    def with_structured_output(self, _schema: type[object], **_kwargs: object) -> RecordingRunnable:
        return self.runnable


def test_provider_passes_only_safe_context_metadata_and_measures_latency(monkeypatch) -> None:
    runnable = RecordingRunnable()
    settings = SimpleNamespace(ai_max_retries=0, ai_model="deepseek-chat")
    provider = LangChainProvider(settings, chat_model=RecordingModel(runnable), sleep=lambda _seconds: None)
    ticks = iter([1_000_000, 4_800_000])
    monkeypatch.setattr("app.ai.provider.time.monotonic_ns", lambda: next(ticks))

    result = provider.invoke_structured(
        "private prompt text",
        NegativeReplyOutput,
        context=AiInvocationContext(job_id=42, job_type="negative_reply", target_reference="comment:8"),
    )

    assert result.latency_ms == 3
    assert runnable.config == {
        "metadata": {"jobId": 42, "jobType": "negative_reply", "targetReference": "comment:8"}
    }
