import json
import re
from typing import Any

from app.ai.context import AiInvocationContext
from app.ai.errors import AiJobCancelledError
from app.ai.chains.negative_reply import NegativeReplyChain
from app.ai.progress import AiJobProgressPublisher
from app.ai.chains.report import ReportChain
from app.ai.provider import LangChainProvider
from app.ai.registry import AiChainRegistry
from app.ai.results import AiInvocationResult
from app.config import settings
from app.rag.reply_service import RagReplyService
from app.rag.report_rag_service import ReportRagService


class AiService:
    def __init__(self, progress_publisher: AiJobProgressPublisher | None = None) -> None:
        self._progress_publisher = progress_publisher or AiJobProgressPublisher()

    def generate_report(self, request: dict[str, Any]) -> dict[str, Any]:
        context = self._start_job(request)
        target_type = request.get("targetType") or "product"
        target_id = request.get("targetId") or ""
        language = request.get("language") or "zh-CN"
        analysis_result = request.get("analysisResult") or {}
        fallback_prompt = (
            "你是中小电商商家的AI运营顾问。请基于评论分析结果生成一份运营报告。"
            "报告标题应为可读的业务标题，不得包含内部商品、商家或目标 ID。"
            "退款、换货、时效和店铺政策只能写成建议或以实际店铺政策为准，不能描述为既有承诺或已完成动作。"
            "必须只输出JSON，不要输出Markdown。JSON字段必须包含："
            "reportTitle, consumerPainPoints, productAdvantages, productDisadvantages, "
            "operationSuggestions, copywritingSuggestions, serviceSuggestions, fullReport。"
            f"目标类型：{target_type}，目标ID：{target_id}，输出语言：{language}。"
            f"评论分析结果：{json.dumps(analysis_result, ensure_ascii=False, default=str)}"
        )
        prompt = self._prompt_from_template(request, fallback_prompt)
        self._publish(context, "retrieving", 30)
        retrieval = self._report_rag_service().retrieve(request=request)
        self._ensure_not_cancelled(context)
        self._publish(context, "generating", 55)
        result = self._generate_report_chain(
            prompt,
            reference_context=retrieval.context if retrieval.context and retrieval.references else None,
            context=context,
        )
        self._publish(context, "validating", 85)
        report_output = result.value
        report = {
            "reportTitle": self._safe_report_title(report_output.report_title, target_type, target_id, language),
            "consumerPainPoints": report_output.consumer_pain_points,
            "productAdvantages": report_output.product_advantages,
            "productDisadvantages": report_output.product_disadvantages,
            "operationSuggestions": report_output.operation_suggestions,
            "copywritingSuggestions": report_output.copywriting_suggestions,
            "serviceSuggestions": report_output.service_suggestions,
            "fullReport": report_output.full_report,
            "ragUsed": bool(retrieval.context and retrieval.references),
            "references": [reference.to_payload() for reference in retrieval.references],
        }
        report.update(self._invocation_metadata(result))
        return {"success": True, "data": report, **self._invocation_metadata(result)}

    def generate_content(self, request: dict[str, Any]) -> dict[str, Any]:
        context = self._start_job(request)
        content_type = request.get("contentType") or "商品文案"
        style_type = request.get("styleType") or "简洁专业"
        language = request.get("language") or "zh-CN"
        target_type = request.get("targetType") or "product"
        target_id = request.get("targetId") or ""
        extra_requirement = request.get("extraRequirement") or ""
        fallback_prompt = (
            "你是电商运营文案专家。请生成可直接给商家使用的营销文案。"
            f"文案类型：{content_type}。风格：{style_type}。语言：{language}。"
            f"目标类型：{target_type}，目标ID：{target_id}。补充要求：{extra_requirement}。"
            "输出正文即可，不要解释生成过程。"
        )
        prompt = self._prompt_from_template(request, fallback_prompt)
        self._publish(context, "generating", 55)
        result = self._generate_content_chain(prompt, context, stream_text=self._streaming_enabled(context))
        self._publish(context, "validating", 85)
        return {
            "success": True,
            "generatedContent": result.value.generated_content,
            **self._invocation_metadata(result),
        }

    def generate_negative_reply(self, request: dict[str, Any]) -> dict[str, Any]:
        context = self._start_job(request)
        comment_id = request.get("commentId") or ""
        review_id = request.get("reviewId") or ""
        product_id = request.get("productId") or ""
        review_score = request.get("reviewScore")
        comment_title = request.get("commentTitle") or ""
        comment_content = request.get("commentContent") or ""
        problem_type = request.get("problemType") or "unknown"
        tone_type = request.get("toneType") or "诚恳专业"
        language = request.get("language") or "zh-CN"
        fallback_prompt = (
            "你是电商客服主管。请为差评生成一段商家回复模板。"
            "回复要真诚、承担责任、给出解决路径，避免争辩和过度承诺。"
            "每条回复都必须针对这条评论单独生成，不要复用通用模板；"
            "需要自然提到客户反馈中的具体问题，不能编造评论中没有的信息。"
            f"语气：{tone_type}。语言：{language}。"
            f"评论ID：{comment_id}。平台评论ID：{review_id}。商品ID：{product_id}。"
            f"评分：{review_score}。问题类型：{problem_type}。"
            f"评论标题：{comment_title}。客户评论：{comment_content}。"
            "如果评论原文缺失，只能基于评分和问题类型表达歉意并引导客服核实。只输出回复内容。"
        )
        prompt = self._prompt_from_template(request, fallback_prompt)
        if settings.ai_negative_reply_engine == "langchain":
            self._publish(context, "retrieving", 30)
            result = self._rag_reply_service().generate(
                request=request,
                rendered_prompt=prompt,
                context=context,
                stream_text=self._streaming_enabled(context),
            )
            self._publish(context, "validating", 85)
            invocation = result.invocation
            return {
                "success": True,
                "replyContent": invocation.value.reply_content,
                "ragUsed": result.rag_used,
                "references": [reference.to_payload() for reference in result.references],
                **self._invocation_metadata(invocation),
            }
        self._publish(context, "generating", 55)
        invocation = self._generate_negative_reply_chain(prompt, context, stream_text=self._streaming_enabled(context))
        self._publish(context, "validating", 85)
        return {
            "success": True,
            "replyContent": invocation.value.reply_content,
            "ragUsed": False,
            "references": [],
            **self._invocation_metadata(invocation),
        }

    def _negative_reply_chain(self) -> NegativeReplyChain:
        return self._chain_registry().create(
            "negative_reply",
            provider=LangChainProvider(
                model_options={
                    "max_tokens": settings.ai_negative_reply_max_tokens,
                    "extra_body": {
                        "thinking": {
                            "type": "enabled" if settings.ai_negative_reply_thinking_enabled else "disabled"
                        }
                    },
                }
            ),
        )

    def _rag_reply_service(self) -> RagReplyService:
        return RagReplyService(reply_chain=self._negative_reply_chain())

    def _report_chain(self) -> ReportChain:
        return self._chain_registry().create("operation_report")

    def _content_generation_chain(self) -> Any:
        return self._chain_registry().create("content_generation")

    def _comment_translation_chain(self) -> Any:
        return self._chain_registry().create("comment_translation")

    def _product_compare_chain(self) -> Any:
        return self._chain_registry().create("product_compare")

    @staticmethod
    def _chain_registry() -> AiChainRegistry:
        return AiChainRegistry()

    def _report_rag_service(self) -> ReportRagService:
        return ReportRagService()

    @staticmethod
    def _safe_report_title(title: str, target_type: str, target_id: str, language: str) -> str:
        normalized_title = title.strip()
        if target_id and target_id.casefold() in normalized_title.casefold():
            return AiService._default_report_title(target_type, language)
        return normalized_title

    @staticmethod
    def _default_report_title(target_type: str, language: str) -> str:
        if language.startswith("zh"):
            return "商家运营分析报告" if target_type == "seller" else "商品运营分析报告"
        if language.startswith("pt"):
            return "Relatório Operacional do Vendedor" if target_type == "seller" else "Relatório Operacional do Produto"
        return "Seller Operations Report" if target_type == "seller" else "Product Operations Report"

    def translate_comment(self, request: dict[str, Any]) -> dict[str, Any]:
        context = self._start_job(request)
        comment_id = request.get("commentId") or ""
        review_id = request.get("reviewId") or ""
        product_id = request.get("productId") or ""
        review_score = request.get("reviewScore")
        comment_title = request.get("commentTitle") or ""
        comment_content = request.get("commentContent") or ""
        target_language = request.get("targetLanguage") or request.get("language") or "zh-CN"
        fallback_prompt = (
            "You are a precise ecommerce review translator. Translate the customer review into the target language. "
            "Preserve product facts, sentiment, complaint details, numbers, and named entities. "
            "Do not add explanations or invented details. "
            "Return only JSON with fields: translatedContent, sourceLanguage. "
            f"target language: {target_language}. "
            f"commentId: {comment_id}. reviewId: {review_id}. productId: {product_id}. "
            f"reviewScore: {review_score}. title: {comment_title}. review: {comment_content}."
        )
        prompt = self._prompt_from_template(request, fallback_prompt)
        self._publish(context, "generating", 55)
        result = self._generate_translation_chain(prompt, context)
        self._publish(context, "validating", 85)
        return {
            "success": True,
            "data": {
                "translatedContent": result.value.translated_content,
                "sourceLanguage": result.value.source_language,
                **self._invocation_metadata(result),
            },
            **self._invocation_metadata(result),
        }

    def generate_product_compare(self, request: dict[str, Any]) -> dict[str, Any]:
        context = self._start_job(request)
        left_product_id = request.get("leftProductId") or ""
        right_product_id = request.get("rightProductId") or ""
        language = request.get("language") or "zh-CN"
        left_analysis = request.get("leftAnalysis") or request.get("leftAnalysisResult") or {}
        right_analysis = request.get("rightAnalysis") or request.get("rightAnalysisResult") or {}
        fallback_prompt = (
            "你是中小电商商家的竞品评论分析顾问。请基于两个商品的评论分析结果生成对比报告。"
            "必须只输出JSON，不要输出Markdown。JSON字段必须包含："
            "compareSummary, advantageAnalysis, riskAnalysis, operationSuggestions。"
            "所有字段值必须是字符串，不能返回嵌套对象或数组。"
            f"左侧商品ID：{left_product_id}。右侧商品ID：{right_product_id}。输出语言：{language}。"
            f"左侧商品分析结果：{json.dumps(left_analysis, ensure_ascii=False, default=str)}"
            f"右侧商品分析结果：{json.dumps(right_analysis, ensure_ascii=False, default=str)}"
        )
        prompt = self._prompt_from_template(request, fallback_prompt)
        self._publish(context, "generating", 55)
        result = self._generate_product_compare_chain(prompt, context)
        self._publish(context, "validating", 85)
        report = {
            "compareSummary": result.value.compare_summary,
            "advantageAnalysis": result.value.advantage_analysis,
            "riskAnalysis": result.value.risk_analysis,
            "operationSuggestions": result.value.operation_suggestions,
            **self._invocation_metadata(result),
        }
        return {"success": True, "data": report, **self._invocation_metadata(result)}

    def _prompt_from_template(self, request: dict[str, Any], fallback_prompt: str) -> str:
        template = request.get("promptTemplate")
        if not isinstance(template, str) or not template.strip():
            return fallback_prompt
        variables = request.get("promptVariables")
        if not isinstance(variables, dict):
            variables = request
        return self._render_template(template, variables)

    def _generate_report_chain(
        self,
        prompt: str,
        *,
        reference_context: str | None,
        context: AiInvocationContext | None,
    ) -> Any:
        chain = self._report_chain()
        if context is None:
            return chain.generate(prompt, reference_context=reference_context)
        return chain.generate(prompt, reference_context=reference_context, context=context)

    def _generate_content_chain(self, prompt: str, context: AiInvocationContext | None, *, stream_text: bool = False) -> Any:
        chain = self._content_generation_chain()
        return chain.generate(prompt, context=context, stream_text=stream_text)

    def _generate_negative_reply_chain(self, prompt: str, context: AiInvocationContext | None, *, stream_text: bool = False) -> Any:
        chain = self._negative_reply_chain()
        return chain.generate(prompt, context=context, stream_text=stream_text)

    @staticmethod
    def _streaming_enabled(context: AiInvocationContext | None) -> bool:
        return bool(
            getattr(settings, "ai_text_streaming_enabled", False)
            and context is not None
            and context.job_type in {"negative_reply", "content"}
        )

    def _generate_translation_chain(self, prompt: str, context: AiInvocationContext | None) -> Any:
        chain = self._comment_translation_chain()
        return chain.generate(prompt) if context is None else chain.generate(prompt, context=context)

    def _generate_product_compare_chain(self, prompt: str, context: AiInvocationContext | None) -> Any:
        chain = self._product_compare_chain()
        return chain.generate(prompt) if context is None else chain.generate(prompt, context=context)

    def _start_job(self, request: dict[str, Any]) -> AiInvocationContext | None:
        job_id = request.get("jobId")
        if not isinstance(job_id, int) or isinstance(job_id, bool) or job_id <= 0:
            return None
        job_type = str(request.get("jobType") or "").strip()
        if not job_type:
            return None
        context = AiInvocationContext(
            job_id=job_id,
            job_type=job_type,
            target_reference=str(request.get("targetReference") or "").strip() or None,
            progress_publisher=self._progress_publisher,
        )
        self._ensure_not_cancelled(context)
        self._publish(context, "preparing", 10)
        return context

    def _publish(self, context: AiInvocationContext | None, stage: str, progress: int) -> None:
        self._progress_publisher.publish(context, stage, progress)

    @staticmethod
    def _ensure_not_cancelled(context: AiInvocationContext | None) -> None:
        publisher = getattr(context, "progress_publisher", None)
        if context is not None and publisher is not None and publisher.is_cancel_requested(context.job_id):
            raise AiJobCancelledError("AI job was cancelled")

    def _render_template(self, template: str, variables: dict[str, Any]) -> str:
        def replace(match: re.Match[str]) -> str:
            key = match.group(1).strip()
            value = variables.get(key)
            if value is None:
                return match.group(0)
            if isinstance(value, (dict, list)):
                return json.dumps(value, ensure_ascii=False, default=str)
            return str(value)

        return re.sub(r"\{([A-Za-z_][A-Za-z0-9_]*)\}", replace, template)

    @staticmethod
    def _invocation_metadata(result: AiInvocationResult[Any]) -> dict[str, Any]:
        return {
            "modelName": result.model_name,
            "tokenUsage": result.total_tokens,
            "inputTokens": result.input_tokens,
            "outputTokens": result.output_tokens,
            "tokenUsageEstimated": result.token_usage_estimated,
            "latencyMs": result.latency_ms,
        }
