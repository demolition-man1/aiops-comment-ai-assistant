from typing import Any

from fastapi import APIRouter

from app.services.comment_analysis_service import CommentAnalysisService
from app.services.comment_ai_shadow_service import CommentAiShadowRequest, CommentAiShadowService

router = APIRouter(prefix="/internal/analysis", tags=["internal-analysis"])


@router.post("/comments")
def analyze_comments(request: dict[str, Any]) -> dict[str, Any]:
    return CommentAnalysisService().analyze_comments(request)


@router.post("/comments/shadow")
def analyze_comments_shadow(request: CommentAiShadowRequest) -> dict[str, Any]:
    return CommentAiShadowService().run(request)
