import json
import re
from typing import Any

import requests

from app.ai.chains.negative_reply import NegativeReplyChain
from app.ai.provider import LangChainProvider
from app.config import settings
from app.rag.reply_service import RagReplyService
from app.rag.report_rag_service import ReportRagService


class AiService:
    def generate_report(self, request: dict[str, Any]) -> dict[str, Any]:
        target_type = request.get("targetType") or "product"
        target_id = request.get("targetId") or ""
        language = request.get("language") or "zh-CN"
        analysis_result = request.get("analysisResult") or {}
        fallback_prompt = (
            "你是中小电商商家的AI运营顾问。请基于评论分析结果生成一份运营报告。"
            "必须只输出JSON，不要输出Markdown。JSON字段必须包含："
            "reportTitle, consumerPainPoints, productAdvantages, productDisadvantages, "
            "operationSuggestions, copywritingSuggestions, serviceSuggestions, fullReport。"
            f"目标类型：{target_type}，目标ID：{target_id}，输出语言：{language}。"
            f"评论分析结果：{json.dumps(analysis_result, ensure_ascii=False, default=str)}"
        )
        prompt = self._prompt_from_template(request, fallback_prompt)
        retrieval = self._report_rag_service().retrieve(request=request)
        if retrieval.context and retrieval.references:
            prompt = self._append_report_references(prompt, retrieval.context)
        content = self._chat(prompt, temperature=0.4)
        parsed = self._parse_json_object(content)
        report = {
            "reportTitle": parsed.get("reportTitle") or "评论驱动型运营分析报告",
            "consumerPainPoints": parsed.get("consumerPainPoints") or "",
            "productAdvantages": parsed.get("productAdvantages") or "",
            "productDisadvantages": parsed.get("productDisadvantages") or "",
            "operationSuggestions": parsed.get("operationSuggestions") or "",
            "copywritingSuggestions": parsed.get("copywritingSuggestions") or "",
            "serviceSuggestions": parsed.get("serviceSuggestions") or "",
            "fullReport": parsed.get("fullReport") or content,
            "modelName": settings.ai_model,
            "tokenUsage": self._estimate_token_usage(prompt, content),
            "ragUsed": bool(retrieval.context and retrieval.references),
            "references": [reference.to_payload() for reference in retrieval.references],
        }
        return {"success": True, "data": report, "tokenUsage": report["tokenUsage"]}

    def generate_content(self, request: dict[str, Any]) -> dict[str, Any]:
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
        content = self._chat(prompt, temperature=0.7)
        return {
            "success": True,
            "generatedContent": content,
            "modelName": settings.ai_model,
            "tokenUsage": self._estimate_token_usage(prompt, content),
        }

    def generate_negative_reply(self, request: dict[str, Any]) -> dict[str, Any]:
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
            result = self._rag_reply_service().generate(request=request, rendered_prompt=prompt)
            return {
                "success": True,
                "replyContent": result.invocation.value.reply_content,
                "modelName": result.invocation.model_name,
                "tokenUsage": result.invocation.total_tokens,
                "ragUsed": result.rag_used,
                "references": [reference.to_payload() for reference in result.references],
            }
        content = self._chat(prompt, temperature=0.75)
        return {
            "success": True,
            "replyContent": content,
            "modelName": settings.ai_model,
            "tokenUsage": self._estimate_token_usage(prompt, content),
            "ragUsed": False,
            "references": [],
        }

    def _negative_reply_chain(self) -> NegativeReplyChain:
        return NegativeReplyChain(
            LangChainProvider(
                model_options={
                    "max_tokens": settings.ai_negative_reply_max_tokens,
                    "extra_body": {
                        "thinking": {
                            "type": "enabled" if settings.ai_negative_reply_thinking_enabled else "disabled"
                        }
                    },
                }
            )
        )

    def _rag_reply_service(self) -> RagReplyService:
        return RagReplyService(reply_chain=self._negative_reply_chain())

    def _report_rag_service(self) -> ReportRagService:
        return ReportRagService()

    def _append_report_references(self, prompt: str, reference_context: str) -> str:
        return (
            f"{prompt}\n\nRetrieved operating evidence follows. Use it only as supporting context. "
            "Do not claim actions or policies that are absent from the evidence, and do not invent source IDs.\n"
            f"{reference_context}"
        )

    def translate_comment(self, request: dict[str, Any]) -> dict[str, Any]:
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
        content = self._chat(prompt, temperature=0.2)
        parsed = self._parse_json_object(content)
        translated_content = self._to_plain_text(parsed.get("translatedContent")) or content
        source_language = self._to_plain_text(parsed.get("sourceLanguage")) or "auto"
        return {
            "success": True,
            "data": {
                "translatedContent": translated_content,
                "sourceLanguage": source_language,
                "modelName": settings.ai_model,
                "tokenUsage": self._estimate_token_usage(prompt, translated_content),
            },
            "tokenUsage": self._estimate_token_usage(prompt, translated_content),
        }

    def generate_product_compare(self, request: dict[str, Any]) -> dict[str, Any]:
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
        content = self._chat(prompt, temperature=0.4)
        parsed = self._parse_json_object(content)
        report = {
            "compareSummary": self._to_plain_text(parsed.get("compareSummary")) or content,
            "advantageAnalysis": self._to_plain_text(parsed.get("advantageAnalysis")),
            "riskAnalysis": self._to_plain_text(parsed.get("riskAnalysis")),
            "operationSuggestions": self._to_plain_text(parsed.get("operationSuggestions")),
            "modelName": settings.ai_model,
            "tokenUsage": self._estimate_token_usage(prompt, content),
        }
        return {"success": True, "data": report, "tokenUsage": report["tokenUsage"]}

    def _prompt_from_template(self, request: dict[str, Any], fallback_prompt: str) -> str:
        template = request.get("promptTemplate")
        if not isinstance(template, str) or not template.strip():
            return fallback_prompt
        variables = request.get("promptVariables")
        if not isinstance(variables, dict):
            variables = request
        return self._render_template(template, variables)

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

    def _estimate_token_usage(self, prompt: str, content: str) -> int:
        # A lightweight demo estimate; the Java side uses it for cost trend display.
        return max(1, (len(prompt or "") + len(content or "")) // 4)

    def _chat(self, prompt: str, temperature: float) -> str:
        if not settings.ai_api_key:
            raise RuntimeError("AI_API_KEY is not configured")
        url = settings.ai_base_url.rstrip("/") + "/" + settings.ai_chat_path.lstrip("/")
        payload = {
            "model": settings.ai_model,
            "messages": [
                {"role": "system", "content": "You are a practical ecommerce operations assistant."},
                {"role": "user", "content": prompt},
            ],
            "temperature": temperature,
        }
        response = requests.post(
            url,
            headers={
                "Authorization": f"Bearer {settings.ai_api_key}",
                "Content-Type": "application/json",
            },
            json=payload,
            timeout=settings.ai_timeout,
        )
        response.raise_for_status()
        data = response.json()
        try:
            return str(data["choices"][0]["message"]["content"]).strip()
        except (KeyError, IndexError, TypeError) as exc:
            raise RuntimeError(f"Unexpected AI response format: {data}") from exc

    def _parse_json_object(self, content: str) -> dict[str, Any]:
        try:
            value = json.loads(content)
            return value if isinstance(value, dict) else {}
        except json.JSONDecodeError:
            pass

        match = re.search(r"\{.*\}", content, flags=re.S)
        if not match:
            return {}
        try:
            value = json.loads(match.group(0))
            return value if isinstance(value, dict) else {}
        except json.JSONDecodeError:
            return {}

    def _to_plain_text(self, value: Any) -> str:
        if value is None:
            return ""
        if isinstance(value, str):
            return value.strip()
        if isinstance(value, dict):
            lines = []
            for key in ("left", "right"):
                if key in value:
                    text = self._to_plain_text(value.get(key))
                    if text:
                        label = "左侧" if key == "left" else "右侧"
                        lines.append(f"{label}：{text}")
            for key, item in value.items():
                if key in {"left", "right"}:
                    continue
                text = self._to_plain_text(item)
                if text:
                    lines.append(f"{key}：{text}")
            return "\n".join(lines)
        if isinstance(value, list):
            return "\n".join(
                text
                for text in (self._to_plain_text(item) for item in value)
                if text
            )
        return str(value).strip()
