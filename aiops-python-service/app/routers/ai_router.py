from typing import Any

from fastapi import APIRouter, HTTPException
from requests import RequestException

from app.services.ai_service import AiService

router = APIRouter(prefix="/internal/ai", tags=["internal-ai"])


@router.post("/report")
def generate_report(request: dict[str, Any]) -> dict[str, Any]:
    return _call_ai(lambda: AiService().generate_report(request))


@router.post("/content")
def generate_content(request: dict[str, Any]) -> dict[str, Any]:
    return _call_ai(lambda: AiService().generate_content(request))


@router.post("/negative-reply")
def generate_negative_reply(request: dict[str, Any]) -> dict[str, Any]:
    return _call_ai(lambda: AiService().generate_negative_reply(request))


@router.post("/comment-translate")
def translate_comment(request: dict[str, Any]) -> dict[str, Any]:
    return _call_ai(lambda: AiService().translate_comment(request))


@router.post("/product-compare")
def generate_product_compare(request: dict[str, Any]) -> dict[str, Any]:
    return _call_ai(lambda: AiService().generate_product_compare(request))


def _call_ai(action: Any) -> dict[str, Any]:
    try:
        return action()
    except RequestException as exc:
        raise HTTPException(status_code=502, detail=f"AI provider request failed: {exc}") from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
