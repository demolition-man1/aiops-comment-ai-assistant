import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient
from unittest.mock import patch

from app.ai.errors import AiAuthenticationError, AiProviderTimeoutError
from app.main import app
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


def test_negative_reply_endpoint_preserves_additive_rag_fields() -> None:
    payload = {
        "success": True,
        "replyContent": "We will check the delivery status.",
        "modelName": "deepseek-chat",
        "tokenUsage": 18,
        "ragUsed": True,
        "references": [
            {"sourceType": "problem_solution", "sourceId": 14, "title": "Delivery guide", "score": 0.91}
        ],
    }
    with patch("app.routers.ai_router.AiService.generate_negative_reply", return_value=payload):
        response = TestClient(app).post("/internal/ai/negative-reply", json={"commentContent": "Late delivery"})

    assert response.status_code == 200
    assert response.json()["ragUsed"] is True
    assert response.json()["references"][0]["sourceId"] == 14
