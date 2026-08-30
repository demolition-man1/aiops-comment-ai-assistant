from dataclasses import dataclass


@dataclass(frozen=True)
class RagRuntimeStatus:
    enabled: bool
    initialized: bool
    embedding_initialized: bool
