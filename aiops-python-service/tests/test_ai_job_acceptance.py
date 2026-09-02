import json

from app.ai.context import AiInvocationContext
from app.ai.progress import AiJobProgressPublisher


class CapturingRedis:
    def __init__(self) -> None:
        self.messages: list[tuple[str, str]] = []

    def publish(self, channel: str, payload: str) -> None:
        self.messages.append((channel, payload))


def test_text_deltas_have_the_cross_service_contract_and_sequence() -> None:
    redis = CapturingRedis()
    publisher = AiJobProgressPublisher(redis_client=redis)
    context = AiInvocationContext(job_id=41, job_type="content")

    assert publisher.publish_text_delta(context, "first ")
    assert publisher.publish_text_delta(context, "second")

    events = [json.loads(payload) for _, payload in redis.messages]
    assert [event["eventType"] for event in events] == ["text_delta", "text_delta"]
    assert [event["jobId"] for event in events] == [41, 41]
    assert [event["jobType"] for event in events] == ["content", "content"]
    assert [event["deltaId"] for event in events] == [1, 2]
    assert [event["text"] for event in events] == ["first ", "second"]


def test_structured_jobs_do_not_publish_text_deltas() -> None:
    redis = CapturingRedis()
    publisher = AiJobProgressPublisher(redis_client=redis)
    context = AiInvocationContext(job_id=42, job_type="operation_report")

    assert not publisher.publish_text_delta(context, "not a validated report")
    assert redis.messages == []
