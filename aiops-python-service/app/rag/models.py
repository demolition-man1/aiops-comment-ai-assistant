from dataclasses import dataclass
from typing import Literal


@dataclass(frozen=True)
class RagRuntimeStatus:
    enabled: bool
    initialized: bool
    embedding_initialized: bool


@dataclass(frozen=True)
class RagReference:
    source_type: Literal["problem_solution", "historical_reply"]
    source_id: int
    title: str | None
    score: float


@dataclass(frozen=True)
class RagRetrievalResult:
    context: str
    references: list[RagReference]


@dataclass(frozen=True)
class RagIndexStatus:
    enabled: bool
    ready: bool
    state: Literal["disabled", "empty", "building", "ready", "failed"]
    collection: str
    document_count: int
    problem_solution_count: int
    historical_reply_count: int
    embedding_model: str
    last_reindex_at: str | None
    last_error: str | None

    def to_payload(self) -> dict[str, object]:
        return {
            "enabled": self.enabled,
            "ready": self.ready,
            "state": self.state,
            "collection": self.collection,
            "documentCount": self.document_count,
            "problemSolutionCount": self.problem_solution_count,
            "historicalReplyCount": self.historical_reply_count,
            "embeddingModel": self.embedding_model,
            "lastReindexAt": self.last_reindex_at,
            "lastError": self.last_error,
        }
