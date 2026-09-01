from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class NegativeReplyOutput(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    reply_content: str = Field(alias="replyContent", min_length=1, max_length=2_000)


class OperationReportOutput(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    report_title: str = Field(alias="reportTitle", min_length=1, max_length=240)
    consumer_pain_points: str = Field(alias="consumerPainPoints", min_length=1, max_length=8_000)
    product_advantages: str = Field(alias="productAdvantages", min_length=1, max_length=8_000)
    product_disadvantages: str = Field(alias="productDisadvantages", min_length=1, max_length=8_000)
    operation_suggestions: str = Field(alias="operationSuggestions", min_length=1, max_length=8_000)
    copywriting_suggestions: str = Field(alias="copywritingSuggestions", min_length=1, max_length=8_000)
    service_suggestions: str = Field(alias="serviceSuggestions", min_length=1, max_length=8_000)
    full_report: str = Field(alias="fullReport", min_length=1, max_length=16_000)

    @field_validator(
        "consumer_pain_points",
        "product_advantages",
        "product_disadvantages",
        "operation_suggestions",
        "copywriting_suggestions",
        "service_suggestions",
        mode="before",
    )
    @classmethod
    def normalize_list_sections(cls, value: object) -> object:
        if not isinstance(value, list):
            return value

        if not value or any(not isinstance(item, str) or not item.strip() for item in value):
            raise ValueError("report sections must contain non-empty strings")

        return "\n".join(f"{index}. {item.strip()}" for index, item in enumerate(value, start=1))


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
