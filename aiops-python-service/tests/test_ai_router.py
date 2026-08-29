import pytest
from fastapi import HTTPException

from app.ai.errors import AiAuthenticationError, AiProviderTimeoutError
from app.routers.ai_router import _call_ai


def test_ai_router_maps_provider_timeout_to_gateway_timeout() -> None:
    with pytest.raises(HTTPException) as captured:
        _call_ai(lambda: (_ for _ in ()).throw(AiProviderTimeoutError("AI provider request timed out")))

    assert captured.value.status_code == 504
    assert captured.value.detail == "AI provider request timed out"


def test_ai_router_maps_authentication_error_without_provider_details() -> None:
    with pytest.raises(HTTPException) as captured:
        _call_ai(lambda: (_ for _ in ()).throw(AiAuthenticationError("AI provider authentication failed")))

    assert captured.value.status_code == 502
    assert captured.value.detail == "AI provider authentication failed"
