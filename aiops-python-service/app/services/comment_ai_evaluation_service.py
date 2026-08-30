from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.utils.evaluation_metrics import evaluate_comment_predictions


class CommentAiEvaluationRow(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    manual_sentiment: Literal["positive", "neutral", "negative"] | None = Field(default=None, alias="manualSentiment")
    manual_problem_types: list[str] = Field(default_factory=list, alias="manualProblemTypes", max_length=5)
    rule_sentiment: Literal["positive", "neutral", "negative"] | None = Field(default=None, alias="ruleSentiment")
    rule_problem_type: str | None = Field(default=None, alias="ruleProblemType", max_length=64)
    ai_sentiment: Literal["positive", "neutral", "negative"] | None = Field(default=None, alias="aiSentiment")
    ai_problems: list[str] = Field(default_factory=list, alias="aiProblems", max_length=5)
    call_status: Literal["pending", "success", "failed"] = Field(default="pending", alias="callStatus")
    json_valid: bool = Field(default=False, alias="jsonValid")
    evidence_valid: bool = Field(default=False, alias="evidenceValid")
    token_usage: int = Field(default=0, ge=0, alias="tokenUsage")
    token_usage_estimated: bool = Field(default=False, alias="tokenUsageEstimated")
    latency_ms: int = Field(default=0, ge=0, alias="latencyMs")

    @field_validator("manual_problem_types", "ai_problems")
    @classmethod
    def normalize_labels(cls, labels: list[str]) -> list[str]:
        normalized = [label.strip().lower() for label in labels if label.strip()]
        if len(normalized) != len(set(normalized)):
            raise ValueError("problem labels must be distinct")
        return normalized


class CommentAiEvaluationRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    rows: list[CommentAiEvaluationRow] = Field(max_length=100)


def evaluate_comment_ai_rows(request: CommentAiEvaluationRequest) -> dict[str, object]:
    rows = [row.model_dump(by_alias=True) for row in request.rows]
    return {"success": True, "evaluation": evaluate_comment_predictions(rows)}
