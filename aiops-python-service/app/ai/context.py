from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class AiInvocationContext:
    job_id: int | None
    job_type: str
    target_reference: str | None = None
    callbacks: tuple[Any, ...] = ()
