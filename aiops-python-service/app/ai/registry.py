from collections.abc import Callable
from typing import Any

from app.ai.chains.comment_analysis import CommentAnalysisChain
from app.ai.chains.comment_translation import CommentTranslationChain
from app.ai.chains.content_generation import ContentGenerationChain
from app.ai.chains.negative_reply import NegativeReplyChain
from app.ai.chains.product_compare import ProductCompareChain
from app.ai.chains.report import ReportChain
from app.ai.provider import LangChainProvider


class AiChainRegistry:
    def __init__(self, provider_factory: Callable[[], Any] = LangChainProvider) -> None:
        self._provider_factory = provider_factory
        self._factories: dict[str, Callable[[Any], Any]] = {
            "operation_report": ReportChain,
            "negative_reply": NegativeReplyChain,
            "product_compare": ProductCompareChain,
            "content_generation": ContentGenerationChain,
            "comment_translation": CommentTranslationChain,
            "comment_analysis": CommentAnalysisChain,
        }

    def create(self, job_type: str, *, provider: Any | None = None) -> Any:
        try:
            chain_factory = self._factories[job_type]
        except KeyError as exception:
            raise ValueError(f"unsupported AI chain: {job_type}") from exception
        return chain_factory(provider or self._provider_factory())
