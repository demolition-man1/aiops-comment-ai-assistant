from dataclasses import dataclass, field
import os
from pathlib import Path
import re


SERVICE_ROOT = Path(__file__).resolve().parents[1]


def _load_dotenv() -> None:
    try:
        from dotenv import load_dotenv

        load_dotenv(dotenv_path=SERVICE_ROOT / ".env")
    except Exception:
        pass


_load_dotenv()


def _int_env(name: str, default: int) -> int:
    value = os.getenv(name)
    if value is None or value == "":
        return default
    return int(value)


def _bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None or value == "":
        return default
    return value.lower() in {"1", "true", "yes", "on"}


def _choice_env(name: str, default: str, allowed: set[str]) -> str:
    value = os.getenv(name, default).strip().lower()
    if value not in allowed:
        supported = ", ".join(sorted(allowed))
        raise ValueError(f"{name} must be one of: {supported}")
    return value


def _non_negative_int_env(name: str, default: int) -> int:
    value = _int_env(name, default)
    if value < 0:
        raise ValueError(f"{name} must be greater than or equal to 0")
    return value


def _bounded_int_env(name: str, default: int, minimum: int, maximum: int) -> int:
    value = _int_env(name, default)
    if value < minimum or value > maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}")
    return value


def _bounded_float_env(name: str, default: float, minimum: float, maximum: float) -> float:
    value = float(os.getenv(name, default))
    if value < minimum or value > maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}")
    return value


def _rag_collection_env(name: str, default: str) -> str:
    value = os.getenv(name, default).strip()
    if not re.fullmatch(r"[a-z0-9][a-z0-9_]{1,61}[a-z0-9]", value):
        raise ValueError(
            f"{name} must use lowercase letters, digits, or underscores and be 3 to 63 characters long"
        )
    return value


def _required_string_env(name: str, default: str) -> str:
    value = os.getenv(name, default).strip()
    if not value:
        raise ValueError(f"{name} must not be empty")
    return value


@dataclass(frozen=True)
class Settings:
    mysql_host: str = os.getenv("MYSQL_HOST", "localhost")
    mysql_port: int = _int_env("MYSQL_PORT", 3306)
    mysql_database: str = os.getenv("MYSQL_DATABASE", "aiops")
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "")

    redis_host: str = os.getenv("REDIS_HOST", "localhost")
    redis_port: int = _int_env("REDIS_PORT", 6379)
    redis_database: int = _non_negative_int_env("REDIS_DATABASE", 0)
    redis_password: str = os.getenv("REDIS_PASSWORD", "")

    ai_provider: str = os.getenv("AI_PROVIDER", "deepseek")
    ai_base_url: str = os.getenv("AI_BASE_URL", "https://api.deepseek.com")
    ai_chat_path: str = os.getenv("AI_CHAT_PATH", "/v1/chat/completions")
    ai_api_key: str = os.getenv("AI_API_KEY", "")
    ai_model: str = os.getenv("AI_MODEL", "deepseek-v4-flash")
    ai_timeout: int = _int_env("AI_TIMEOUT", 30)
    ai_text_streaming_enabled: bool = field(
        default_factory=lambda: _bool_env("AI_TEXT_STREAMING_ENABLED", False)
    )
    ai_negative_reply_engine: str = field(
        default_factory=lambda: _choice_env(
            "AI_NEGATIVE_REPLY_ENGINE", "langchain", {"langchain", "legacy"}
        )
    )
    ai_negative_reply_thinking_enabled: bool = field(
        default_factory=lambda: _bool_env("AI_NEGATIVE_REPLY_THINKING_ENABLED", False)
    )
    ai_negative_reply_max_tokens: int = field(
        default_factory=lambda: _bounded_int_env("AI_NEGATIVE_REPLY_MAX_TOKENS", 320, 64, 1024)
    )
    ai_max_retries: int = field(default_factory=lambda: _non_negative_int_env("AI_MAX_RETRIES", 2))
    comment_ai_shadow_default_sample_size: int = field(
        default_factory=lambda: _bounded_int_env("COMMENT_AI_SHADOW_DEFAULT_SAMPLE_SIZE", 60, 1, 100)
    )
    comment_ai_shadow_max_sample_size: int = field(
        default_factory=lambda: _bounded_int_env("COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE", 100, 1, 100)
    )
    comment_ai_shadow_default_max_total_tokens: int = field(
        default_factory=lambda: _bounded_int_env("COMMENT_AI_SHADOW_DEFAULT_MAX_TOTAL_TOKENS", 60000, 1000, 100000)
    )
    comment_ai_shadow_max_total_tokens: int = field(
        default_factory=lambda: _bounded_int_env("COMMENT_AI_SHADOW_MAX_TOTAL_TOKENS", 100000, 1000, 100000)
    )
    comment_ai_mode: str = field(
        default_factory=lambda: _choice_env("COMMENT_AI_MODE", "rule", {"rule", "hybrid"})
    )
    comment_ai_hybrid_min_confidence: float = field(
        default_factory=lambda: _bounded_float_env("COMMENT_AI_HYBRID_MIN_CONFIDENCE", 0.80, 0.0, 1.0)
    )
    rag_enabled: bool = field(default_factory=lambda: _bool_env("RAG_ENABLED", False))
    rag_collection: str = field(
        default_factory=lambda: _rag_collection_env("RAG_COLLECTION", "aiops_knowledge_v1")
    )
    rag_top_k: int = field(default_factory=lambda: _bounded_int_env("RAG_TOP_K", 4, 1, 10))
    rag_min_relevance_score: float = field(
        default_factory=lambda: _bounded_float_env("RAG_MIN_RELEVANCE_SCORE", 0.35, 0.0, 1.0)
    )
    rag_max_context_chars: int = field(
        default_factory=lambda: _bounded_int_env("RAG_MAX_CONTEXT_CHARS", 6000, 500, 12000)
    )
    rag_reply_top_k: int = field(
        default_factory=lambda: _bounded_int_env("RAG_REPLY_TOP_K", 2, 1, 10)
    )
    rag_reply_max_context_chars: int = field(
        default_factory=lambda: _bounded_int_env("RAG_REPLY_MAX_CONTEXT_CHARS", 1800, 500, 12000)
    )
    rag_review_evidence_max_documents: int = field(
        default_factory=lambda: _bounded_int_env("RAG_REVIEW_EVIDENCE_MAX_DOCUMENTS", 2000, 0, 10000)
    )
    rag_chroma_dir: str = field(
        default_factory=lambda: _required_string_env("RAG_CHROMA_DIR", "./data/chroma")
    )
    embedding_model: str = field(
        default_factory=lambda: _required_string_env("EMBEDDING_MODEL", "intfloat/multilingual-e5-small")
    )
    embedding_device: str = field(
        default_factory=lambda: _choice_env("EMBEDDING_DEVICE", "cpu", {"cpu", "cuda", "mps"})
    )
    local_import_host_path: str = os.getenv("LOCAL_IMPORT_HOST_PATH", "").strip()
    local_import_container_path: str = os.getenv("LOCAL_IMPORT_CONTAINER_PATH", "/data/local-import").strip()

    crawler_enabled: bool = _bool_env("CRAWLER_ENABLED", False)

    def __post_init__(self) -> None:
        if self.comment_ai_shadow_default_sample_size > self.comment_ai_shadow_max_sample_size:
            raise ValueError("COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE must be at least the default sample size")
        if self.comment_ai_shadow_default_max_total_tokens > self.comment_ai_shadow_max_total_tokens:
            raise ValueError("COMMENT_AI_SHADOW_MAX_TOTAL_TOKENS must be at least the default token budget")


settings = Settings()
