from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class NegativeReplyOutput(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    reply_content: str = Field(alias="replyContent", min_length=1, max_length=2_000)


class CommentProblem(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    type: str = Field(min_length=1, max_length=64)
    confidence: float = Field(ge=0.0, le=1.0)
    evidence: str = Field(min_length=1, max_length=240)


class CommentAnalysisOutput(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    sentiment: Literal["positive", "neutral", "negative"]
    sentiment_confidence: float = Field(alias="sentimentConfidence", ge=0.0, le=1.0)
    primary_problem: str | None = Field(default=None, alias="primaryProblem", max_length=64)
    problems: list[CommentProblem] = Field(default_factory=list, max_length=5)

    @model_validator(mode="after")
    def validate_primary_problem(self) -> "CommentAnalysisOutput":
        if self.primary_problem is not None and self.primary_problem not in {problem.type for problem in self.problems}:
            raise ValueError("primaryProblem must match one of the provided problems")
        return self
