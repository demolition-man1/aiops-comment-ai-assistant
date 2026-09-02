import json
from collections.abc import Callable
from datetime import datetime, timezone
from typing import Any

from app.ai.context import AiInvocationContext
from app.config import Settings, settings


class AiJobProgressPublisher:
    EVENT_CHANNEL = "aiops:ai-job-events"
    CANCEL_KEY_PREFIX = "aiops:ai-job-cancel:"
    _STAGES = frozenset({"preparing", "retrieving", "generating", "validating", "persisting"})

    def __init__(
        self,
        redis_client: Any | None = None,
        provider_settings: Settings = settings,
        clock: Callable[[], datetime] | None = None,
    ) -> None:
        self._redis_client = redis_client
        self._settings = provider_settings
        self._clock = clock or (lambda: datetime.now(timezone.utc))
        self._progress_by_job: dict[int, int] = {}
        self._text_sequence_by_job: dict[int, int] = {}

    def publish(self, context: AiInvocationContext | None, stage: str, progress: int) -> bool:
        if context is None or context.job_id is None or context.job_id <= 0:
            return False
        if stage not in self._STAGES or progress < 0 or progress > 100:
            return False
        previous = self._progress_by_job.get(context.job_id, -1)
        if progress < previous:
            return False
        payload = {
            "eventType": "stage",
            "jobId": context.job_id,
            "jobType": context.job_type,
            "occurredAt": self._clock().astimezone(timezone.utc).isoformat(),
            "progress": progress,
            "stage": stage,
        }
        try:
            self._client().publish(self.EVENT_CHANNEL, json.dumps(payload, ensure_ascii=False, separators=(",", ":")))
        except Exception:
            return False
        self._progress_by_job[context.job_id] = progress
        return True

    def publish_text_delta(self, context: AiInvocationContext | None, text: str) -> bool:
        if context is None or context.job_id is None or context.job_id <= 0:
            return False
        if context.job_type not in {"negative_reply", "content"} or not text:
            return False
        sequence = self._text_sequence_by_job.get(context.job_id, 0) + 1
        payload = {
            "eventType": "text_delta",
            "jobId": context.job_id,
            "jobType": context.job_type,
            "occurredAt": self._clock().astimezone(timezone.utc).isoformat(),
            "text": text,
            "deltaId": sequence,
        }
        try:
            self._client().publish(self.EVENT_CHANNEL, json.dumps(payload, ensure_ascii=False, separators=(",", ":")))
        except Exception:
            return False
        self._text_sequence_by_job[context.job_id] = sequence
        return True

    def is_cancel_requested(self, job_id: int | None) -> bool:
        if job_id is None or job_id <= 0:
            return False
        try:
            return self._client().get(f"{self.CANCEL_KEY_PREFIX}{job_id}") is not None
        except Exception:
            return False

    def _client(self) -> Any:
        if self._redis_client is None:
            import redis

            self._redis_client = redis.Redis(
                host=self._settings.redis_host,
                port=self._settings.redis_port,
                db=self._settings.redis_database,
                password=self._settings.redis_password or None,
                decode_responses=True,
                socket_connect_timeout=1,
                socket_timeout=1,
            )
        return self._redis_client
