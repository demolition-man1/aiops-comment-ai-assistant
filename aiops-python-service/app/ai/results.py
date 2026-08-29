from dataclasses import dataclass
from typing import Generic, TypeVar


T = TypeVar("T")


@dataclass(frozen=True)
class AiInvocationResult(Generic[T]):
    value: T
    model_name: str
    input_tokens: int | None
    output_tokens: int | None
    total_tokens: int
    token_usage_estimated: bool
