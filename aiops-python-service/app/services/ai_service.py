import json
import re
from typing import Any

import requests

from app.config import settings


class AiService:
    def generate_report(self, request: dict[str, Any]) -> dict[str, Any]:
        target_type = request.get("targetType") or "product"
        target_id = request.get("targetId") or ""
        language = request.get("language") or "zh-CN"
        analysis_result = request.get("analysisResult") or {}
        prompt = (
            "你是中小电商商家的AI运营顾问。请基于评论分析结果生成一份运营报告。"
            "必须只输出JSON，不要输出Markdown。JSON字段必须包含："
            "reportTitle, consumerPainPoints, productAdvantages, productDisadvantages, "
            "operationSuggestions, copywritingSuggestions, serviceSuggestions, fullReport。"
            f"目标类型：{target_type}，目标ID：{target_id}，输出语言：{language}。"
            f"评论分析结果：{json.dumps(analysis_result, ensure_ascii=False, default=str)}"
        )
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
        }
        return {"success": True, "data": report}

    def generate_content(self, request: dict[str, Any]) -> dict[str, Any]:
        content_type = request.get("contentType") or "商品文案"
        style_type = request.get("styleType") or "简洁专业"
        language = request.get("language") or "zh-CN"
        target_type = request.get("targetType") or "product"
        target_id = request.get("targetId") or ""
        extra_requirement = request.get("extraRequirement") or ""
        prompt = (
            "你是电商运营文案专家。请生成可直接给商家使用的营销文案。"
            f"文案类型：{content_type}。风格：{style_type}。语言：{language}。"
            f"目标类型：{target_type}，目标ID：{target_id}。补充要求：{extra_requirement}。"
            "输出正文即可，不要解释生成过程。"
        )
        content = self._chat(prompt, temperature=0.7)
        return {"success": True, "generatedContent": content, "modelName": settings.ai_model}

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
        prompt = (
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
        content = self._chat(prompt, temperature=0.75)
        return {"success": True, "replyContent": content, "modelName": settings.ai_model}

    def generate_product_compare(self, request: dict[str, Any]) -> dict[str, Any]:
        left_product_id = request.get("leftProductId") or ""
        right_product_id = request.get("rightProductId") or ""
        language = request.get("language") or "zh-CN"
        left_analysis = request.get("leftAnalysis") or request.get("leftAnalysisResult") or {}
        right_analysis = request.get("rightAnalysis") or request.get("rightAnalysisResult") or {}
        prompt = (
            "你是中小电商商家的竞品评论分析顾问。请基于两个商品的评论分析结果生成对比报告。"
            "必须只输出JSON，不要输出Markdown。JSON字段必须包含："
            "compareSummary, advantageAnalysis, riskAnalysis, operationSuggestions。"
            "所有字段值必须是字符串，不能返回嵌套对象或数组。"
            f"左侧商品ID：{left_product_id}。右侧商品ID：{right_product_id}。输出语言：{language}。"
            f"左侧商品分析结果：{json.dumps(left_analysis, ensure_ascii=False, default=str)}"
            f"右侧商品分析结果：{json.dumps(right_analysis, ensure_ascii=False, default=str)}"
        )
        content = self._chat(prompt, temperature=0.4)
        parsed = self._parse_json_object(content)
        report = {
            "compareSummary": self._to_plain_text(parsed.get("compareSummary")) or content,
            "advantageAnalysis": self._to_plain_text(parsed.get("advantageAnalysis")),
            "riskAnalysis": self._to_plain_text(parsed.get("riskAnalysis")),
            "operationSuggestions": self._to_plain_text(parsed.get("operationSuggestions")),
            "modelName": settings.ai_model,
        }
        return {"success": True, "data": report}

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
