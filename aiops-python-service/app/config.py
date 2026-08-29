from dataclasses import dataclass, field
import os
from pathlib import Path


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


@dataclass(frozen=True)
class Settings:
    mysql_host: str = os.getenv("MYSQL_HOST", "localhost")
    mysql_port: int = _int_env("MYSQL_PORT", 3306)
    mysql_database: str = os.getenv("MYSQL_DATABASE", "aiops")
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "")

    ai_provider: str = os.getenv("AI_PROVIDER", "deepseek")
    ai_base_url: str = os.getenv("AI_BASE_URL", "https://api.deepseek.com")
    ai_chat_path: str = os.getenv("AI_CHAT_PATH", "/v1/chat/completions")
    ai_api_key: str = os.getenv("AI_API_KEY", "")
    ai_model: str = os.getenv("AI_MODEL", "deepseek-chat")
    ai_timeout: int = _int_env("AI_TIMEOUT", 30)
    ai_negative_reply_engine: str = field(
        default_factory=lambda: _choice_env(
            "AI_NEGATIVE_REPLY_ENGINE", "langchain", {"langchain", "legacy"}
        )
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

    crawler_enabled: bool = _bool_env("CRAWLER_ENABLED", False)

    def __post_init__(self) -> None:
        if self.comment_ai_shadow_default_sample_size > self.comment_ai_shadow_max_sample_size:
            raise ValueError("COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE must be at least the default sample size")
        if self.comment_ai_shadow_default_max_total_tokens > self.comment_ai_shadow_max_total_tokens:
            raise ValueError("COMMENT_AI_SHADOW_MAX_TOTAL_TOKENS must be at least the default token budget")


settings = Settings()
