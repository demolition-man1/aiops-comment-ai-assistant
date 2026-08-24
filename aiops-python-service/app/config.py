from dataclasses import dataclass
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


@dataclass(frozen=True)
class Settings:
    mysql_host: str = os.getenv("MYSQL_HOST", "localhost")
    mysql_port: int = _int_env("MYSQL_PORT", 3306)
    mysql_database: str = os.getenv("MYSQL_DATABASE", "aiops")
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "432")

    ai_provider: str = os.getenv("AI_PROVIDER", "deepseek")
    ai_base_url: str = os.getenv("AI_BASE_URL", "https://api.deepseek.com")
    ai_chat_path: str = os.getenv("AI_CHAT_PATH", "/v1/chat/completions")
    ai_api_key: str = os.getenv("AI_API_KEY", "")
    ai_model: str = os.getenv("AI_MODEL", "deepseek-chat")
    ai_timeout: int = _int_env("AI_TIMEOUT", 30)

    crawler_enabled: bool = _bool_env("CRAWLER_ENABLED", False)


settings = Settings()
