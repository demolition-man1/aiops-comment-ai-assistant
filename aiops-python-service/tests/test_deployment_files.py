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
