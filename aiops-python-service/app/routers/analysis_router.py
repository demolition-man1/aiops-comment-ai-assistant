from typing import Any

from fastapi import APIRouter

from app.services.comment_analysis_service import CommentAnalysisService

router = APIRouter(prefix="/internal/analysis", tags=["internal-analysis"])


@router.post("/comments")
def analyze_comments(request: dict[str, Any]) -> dict[str, Any]:
    return CommentAnalysisService().analyze_comments(request)
