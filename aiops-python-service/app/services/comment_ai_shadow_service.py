from __future__ import annotations

import time
from collections.abc import Callable
from contextlib import AbstractContextManager
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

from app.ai.chains.comment_analysis import CommentAnalysisChain
from app.ai.errors import AiAuthenticationError, AiConfigurationError, AiServiceError
from app.ai.provider import LangChainProvider
from app.config import Settings, settings
from app.db import get_conn
from app.repositories import comment_ai_shadow_repository, comment_repository, task_repository
from app.utils.shadow_sampler import select_shadow_sample


class CommentAiShadowRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

    task_id: int = Field(alias="taskId", gt=0)
    run_id: int = Field(alias="runId", gt=0)
    target_type: Literal["product", "seller"] = Field(alias="targetType")
    target_id: str = Field(alias="targetId", min_length=1, max_length=64)
    sample_size: int = Field(alias="sampleSize", ge=1, le=100)
    sample_seed: int = Field(alias="sampleSeed")
    max_total_tokens: int = Field(alias="maxTotalTokens", ge=1000, le=100000)
    prompt_template: str = Field(alias="promptTemplate", min_length=1)
    prompt_variables: dict[str, Any] = Field(default_factory=dict, alias="promptVariables")


class CommentAiShadowService:
    _BATCH_SIZE = 10

    def __init__(
        self,
        provider: Any | None = None,
        connection_factory: Callable[[], AbstractContextManager[Any]] = get_conn,
        service_settings: Settings = settings,
    ) -> None:
        self._chain = CommentAnalysisChain(provider or LangChainProvider(service_settings))
        self._connection_factory = connection_factory
        self._settings = service_settings

    def run(self, raw_request: CommentAiShadowRequest | dict[str, Any]) -> dict[str, Any]:
        request = raw_request if isinstance(raw_request, CommentAiShadowRequest) else CommentAiShadowRequest.model_validate(raw_request)
        self._validate_limits(request)
        with self._connection_factory() as conn:
            candidates = comment_repository.fetch_shadow_candidates(conn, request.target_type, request.target_id)
            sample = select_shadow_sample(candidates, request.sample_size, request.sample_seed)
            comment_ai_shadow_repository.reserve_shadow_results(conn, request.run_id, sample)
            comment_ai_shadow_repository.start_shadow_run(conn, request.run_id, len(sample))
            pending = comment_ai_shadow_repository.fetch_pending_shadow_results(conn, request.run_id)

        total_calls = 0
        success_count = 0
        failure_count = 0
        total_tokens = 0
        total_latency_ms = 0
        model_name: str | None = None
        terminal_error: str | None = None
        status = "success"
        pending_updates: list[dict[str, Any]] = []
        processed = 0

        for row in pending:
            if total_tokens >= request.max_total_tokens:
                status = "budget_stopped"
                break
            started_at = time.perf_counter()
            try:
                result = self._chain.generate(
                    self._review_text(row),
                    int(row.get("review_score") or 0),
                    self._render_prompt(request, row),
                )
                latency_ms = self._elapsed_ms(started_at)
                total_calls += 1
                success_count += 1
                total_tokens += result.total_tokens
                total_latency_ms += latency_ms
                model_name = result.model_name
                pending_updates.append(comment_ai_shadow_repository.success_shadow_result_update(
                    request.run_id,
                    int(row["comment_id"]),
                    result.value,
                    result.model_name,
                    result.total_tokens,
                    result.token_usage_estimated,
                    latency_ms,
                ))
            except (AiConfigurationError, AiAuthenticationError) as error:
                latency_ms = self._elapsed_ms(started_at)
                total_calls += 1
                failure_count += 1
                total_latency_ms += latency_ms
                terminal_error = error.public_message
                status = "failed"
                pending_updates.append(comment_ai_shadow_repository.failed_shadow_result_update(
                    request.run_id,
                    int(row["comment_id"]),
                    error.public_message,
                    self._error_tokens(error),
                    self._error_tokens_estimated(error),
                    latency_ms,
                ))
                processed += 1
                break
            except Exception as error:
                latency_ms = self._elapsed_ms(started_at)
                total_calls += 1
                failure_count += 1
                total_tokens += self._error_tokens(error)
                total_latency_ms += latency_ms
                error_message = self._error_message(error)
                pending_updates.append(comment_ai_shadow_repository.failed_shadow_result_update(
                    request.run_id,
                    int(row["comment_id"]),
                    error_message,
                    self._error_tokens(error),
                    self._error_tokens_estimated(error),
                    latency_ms,
                ))

            processed += 1
            if len(pending_updates) == self._BATCH_SIZE:
                self._persist_batch(pending_updates, request.task_id, processed, len(sample))
                pending_updates = []

        if pending_updates:
            self._persist_batch(pending_updates, request.task_id, processed, len(sample))

        if status == "success" and failure_count > 0:
            status = "partial"
        if not pending and not sample:
            status = "success"

        with self._connection_factory() as conn:
            comment_ai_shadow_repository.finish_shadow_run(
                conn,
                request.run_id,
                status,
                total_calls,
                success_count,
                failure_count,
                total_tokens,
                total_latency_ms,
                terminal_error,
            )
            task_repository.update_analysis_task(
                conn,
                request.task_id,
                status,
                100,
                terminal_error,
            )

        response = {
            "success": status != "failed",
            "runId": request.run_id,
            "status": status,
            "actualSampleSize": len(sample),
            "successCount": success_count,
            "failureCount": failure_count,
            "totalCalls": total_calls,
            "totalTokens": total_tokens,
            "modelName": model_name,
        }
        if terminal_error:
            response["message"] = terminal_error
        return response

    def _validate_limits(self, request: CommentAiShadowRequest) -> None:
        if request.sample_size > self._settings.comment_ai_shadow_max_sample_size:
            raise ValueError("sampleSize exceeds COMMENT_AI_SHADOW_MAX_SAMPLE_SIZE")
        if request.max_total_tokens > self._settings.comment_ai_shadow_max_total_tokens:
            raise ValueError("maxTotalTokens exceeds COMMENT_AI_SHADOW_MAX_TOTAL_TOKENS")

    @staticmethod
    def _review_text(row: dict[str, Any]) -> str:
        return str(row.get("clean_content") or row.get("review_content") or "").strip()

    @staticmethod
    def _elapsed_ms(started_at: float) -> int:
        return max(0, round((time.perf_counter() - started_at) * 1000))

    def _render_prompt(self, request: CommentAiShadowRequest, row: dict[str, Any]) -> str:
        variables = dict(request.prompt_variables)
        variables.update(
            reviewScore=row.get("review_score") or 0,
            reviewText=self._review_text(row),
        )
        return request.prompt_template.format_map(_PromptVariables(variables))

    @staticmethod
    def _error_message(error: Exception) -> str:
        if isinstance(error, AiServiceError):
            return error.public_message
        return "Comment AI shadow analysis failed"

    @staticmethod
    def _error_tokens(error: Exception) -> int:
        return int(getattr(error, "total_tokens", 0) or 0)

    @staticmethod
    def _error_tokens_estimated(error: Exception) -> bool:
        return bool(getattr(error, "token_usage_estimated", False))

    @staticmethod
    def _update_progress(conn: Any, task_id: int, processed: int, total: int, status: str) -> None:
        progress = 0 if total == 0 else min(99, int(processed * 100 / total))
        task_repository.update_analysis_task(conn, task_id, status, progress)

    def _persist_batch(self, updates: list[dict[str, Any]], task_id: int, processed: int, total: int) -> None:
        with self._connection_factory() as conn:
            comment_ai_shadow_repository.persist_shadow_result_batch(conn, updates)
            self._update_progress(conn, task_id, processed, total, "processing")


class _PromptVariables(dict[str, Any]):
    def __missing__(self, key: str) -> str:
        return "{" + key + "}"
