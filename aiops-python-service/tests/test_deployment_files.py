from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]


def test_compose_stack_wires_all_runtime_services() -> None:
    compose = (REPOSITORY_ROOT / "compose.yml").read_text(encoding="utf-8")

    for service in ("frontend:", "backend:", "python-service:", "mysql:", "redis:"):
        assert service in compose
    assert "condition: service_healthy" in compose
    assert "AIOPS_PYTHON_BASE_URL: http://python-service:8001" in compose
    assert "MYSQL_HOST: mysql" in compose
    assert "AIOPS_MYSQL_PASSWORD" in compose
    assert "AI_API_KEY" in compose


def test_compose_uses_3307_for_mysql_host_port_only() -> None:
    compose = (REPOSITORY_ROOT / "compose.yml").read_text(encoding="utf-8")
    environment = (REPOSITORY_ROOT / ".env.example").read_text(encoding="utf-8")

    assert '${AIOPS_MYSQL_PORT:-3307}:3306' in compose
    assert "MYSQL_PORT: 3306" in compose
    assert "AIOPS_MYSQL_PORT: 3306" in compose
    assert "AIOPS_MYSQL_PORT=3307" in environment


def test_compose_passes_langchain_negative_reply_configuration() -> None:
    compose = (REPOSITORY_ROOT / "compose.yml").read_text(encoding="utf-8")
    environment = (REPOSITORY_ROOT / ".env.example").read_text(encoding="utf-8")

    assert "AI_NEGATIVE_REPLY_ENGINE" in compose
    assert "AI_MAX_RETRIES" in compose
    assert "AI_NEGATIVE_REPLY_ENGINE=langchain" in environment
    assert "AI_MAX_RETRIES=2" in environment


def test_compose_passes_lazy_rag_runtime_configuration() -> None:
    compose = (REPOSITORY_ROOT / "compose.yml").read_text(encoding="utf-8")
    environment = (REPOSITORY_ROOT / ".env.example").read_text(encoding="utf-8")
    python_environment = (REPOSITORY_ROOT / "aiops-python-service" / ".env.example").read_text(encoding="utf-8")

    for name in (
        "RAG_ENABLED",
        "RAG_COLLECTION",
        "RAG_TOP_K",
        "RAG_MIN_RELEVANCE_SCORE",
        "RAG_MAX_CONTEXT_CHARS",
        "RAG_CHROMA_DIR",
        "EMBEDDING_MODEL",
        "EMBEDDING_DEVICE",
    ):
        assert name in compose
        assert name in environment
        assert name in python_environment

    assert "RAG_ENABLED=false" in environment
    assert "RAG_ENABLED=false" in python_environment


def test_images_and_nginx_proxy_are_declared() -> None:
    backend_dockerfile = (REPOSITORY_ROOT / "aiops-backend" / "Dockerfile").read_text(encoding="utf-8")
    python_dockerfile = (REPOSITORY_ROOT / "aiops-python-service" / "Dockerfile").read_text(encoding="utf-8")
    frontend_dockerfile = (REPOSITORY_ROOT / "aiops-frontend" / "Dockerfile").read_text(encoding="utf-8")
    nginx_config = (REPOSITORY_ROOT / "aiops-frontend" / "nginx.conf").read_text(encoding="utf-8")

    assert "eclipse-temurin:21" in backend_dockerfile
    assert "python:3.11" in python_dockerfile
    assert "nginx:" in frontend_dockerfile
    assert "proxy_pass http://backend:8080" in nginx_config
    assert "try_files $uri $uri/ /index.html" in nginx_config


def test_frontend_healthcheck_uses_ipv4_loopback() -> None:
    compose = (REPOSITORY_ROOT / "compose.yml").read_text(encoding="utf-8")
    frontend_dockerfile = (REPOSITORY_ROOT / "aiops-frontend" / "Dockerfile").read_text(encoding="utf-8")

    assert "http://127.0.0.1/healthz" in compose
    assert "http://127.0.0.1/healthz" in frontend_dockerfile


def test_example_environment_contains_placeholders_only() -> None:
    environment = (REPOSITORY_ROOT / ".env.example").read_text(encoding="utf-8")

    for name in (
        "AIOPS_MYSQL_PASSWORD",
        "AIOPS_JWT_SECRET",
        "AI_API_KEY",
        "ALIYUN_OSS_ACCESS_KEY_ID",
        "ALIYUN_OSS_ACCESS_KEY_SECRET",
    ):
        assert f"{name}=" in environment
    assert "replace-with-" in environment
