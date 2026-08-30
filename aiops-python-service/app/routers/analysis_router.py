from typing import Any

from fastapi import APIRouter

from app.services.comment_analysis_service import CommentAnalysisService
from app.services.comment_ai_evaluation_service import CommentAiEvaluationRequest, evaluate_comment_ai_rows
from app.services.comment_ai_shadow_service import CommentAiShadowRequest, CommentAiShadowService

router = APIRouter(prefix="/internal/analysis", tags=["internal-analysis"])


@router.post("/comments")
def analyze_comments(request: dict[str, Any]) -> dict[str, Any]:
    return CommentAnalysisService().analyze_comments(request)


@router.post("/comments/shadow")
def analyze_comments_shadow(request: CommentAiShadowRequest) -> dict[str, Any]:
    return CommentAiShadowService().run(request)


@router.post("/comments/evaluation")
def evaluate_comments(request: CommentAiEvaluationRequest) -> dict[str, object]:
    return evaluate_comment_ai_rows(request)
