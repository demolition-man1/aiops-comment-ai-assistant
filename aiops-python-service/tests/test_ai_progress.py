import json
from datetime import datetime, timezone

from app.ai.context import AiInvocationContext
from app.ai.progress import AiJobProgressPublisher


class FakeRedis:
    def __init__(self) -> None:
        self.messages: list[tuple[str, str]] = []
        self.values: dict[str, str] = {}

    def publish(self, channel: str, message: str) -> int:
        self.messages.append((channel, message))
        return 1

    def get(self, key: str) -> str | None:
        return self.values.get(key)


def test_publisher_emits_only_safe_monotonic_job_events() -> None:
    redis = FakeRedis()
    publisher = AiJobProgressPublisher(
        redis_client=redis,
        clock=lambda: datetime(2026, 9, 1, tzinfo=timezone.utc),
    )
    context = AiInvocationContext(job_id=81, job_type="operation_report", target_reference="product:7")

    assert publisher.publish(context, "preparing", 10)
    assert publisher.publish(context, "generating", 55)
    assert not publisher.publish(context, "generating", 54)
    assert not publisher.publish(context, "unknown", 80)

    assert len(redis.messages) == 2
    channel, message = redis.messages[-1]
    assert channel == "aiops:ai-job-events"
    assert json.loads(message) == {
        "eventType": "stage",
        "jobId": 81,
        "jobType": "operation_report",
        "occurredAt": "2026-09-01T00:00:00+00:00",
        "progress": 55,
        "stage": "generating",
    }


def test_publisher_checks_durable_cancellation_flag() -> None:
    redis = FakeRedis()
    redis.values["aiops:ai-job-cancel:81"] = "1"
    publisher = AiJobProgressPublisher(redis_client=redis)

    assert publisher.is_cancel_requested(81)
    assert not publisher.is_cancel_requested(82)
    assert not publisher.is_cancel_requested(None)
