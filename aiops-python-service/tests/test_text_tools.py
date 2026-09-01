import unittest
from decimal import Decimal
from types import SimpleNamespace
from unittest.mock import patch

import pandas as pd

from app.ai.results import AiInvocationResult
from app.ai.schemas import (
    CommentTranslationOutput,
    ContentGenerationOutput,
    NegativeReplyOutput,
    ProductCompareOutput,
)
from app.utils.keyword_extractor import extract_keywords, keyword_rank
from app.utils.problem_classifier import classify_problem
from app.utils.sentiment_analyzer import sentiment_from_score
from app.utils.text_cleaner import clean_text
from app.utils.topic_clusterer import topic_distribution
from app.services.comment_analysis_service import aggregate_custom_tags, build_problem_types, build_trend_distribution
from app.services.ai_service import AiService
from app.services.olist_import_service import OlistImportService


class FakeNegativeReplyChain:
    def __init__(self, reply_content: str, token_usage: int = 24) -> None:
        self.reply_content = reply_content
        self.token_usage = token_usage
        self.prompts: list[str] = []

    def generate(self, prompt: str, **_kwargs: object) -> AiInvocationResult[NegativeReplyOutput]:
        self.prompts.append(prompt)
        return AiInvocationResult(
            value=NegativeReplyOutput.model_validate({"replyContent": self.reply_content}),
            model_name="deepseek-chat",
            input_tokens=14,
            output_tokens=10,
            total_tokens=self.token_usage,
            token_usage_estimated=False,
        )


class FakeStructuredChain:
    def __init__(self, result: AiInvocationResult[object]) -> None:
        self.result = result
        self.prompts: list[str] = []

    def generate(self, prompt: str, **_kwargs: object) -> AiInvocationResult[object]:
        self.prompts.append(prompt)
        return self.result


class TextToolTests(unittest.TestCase):
    def test_clean_text_removes_html_and_urls(self) -> None:
        value = clean_text("<p>good product</p> https://example.com")
        self.assertEqual(value, "good product")

    def test_sentiment_from_score(self) -> None:
        self.assertEqual(sentiment_from_score(1), ("negative", Decimal("0.2000"), 1))
        self.assertEqual(sentiment_from_score(3), ("neutral", Decimal("0.5000"), 0))
        self.assertEqual(sentiment_from_score(5), ("positive", Decimal("0.8500"), 0))

    def test_problem_classifier(self) -> None:
        self.assertEqual(classify_problem("entrega com atraso", 1), "logistics")
        self.assertEqual(classify_problem("produto quebrado", 1), "quality")
        self.assertIsNone(classify_problem("produto bom", 0))

    def test_keyword_tools(self) -> None:
        self.assertIn("produto", extract_keywords("produto produto bom", limit=2))
        ranked = keyword_rank(["produto bom", "produto excelente"], limit=1)
        self.assertEqual(ranked[0]["keyword"], "produto")
        self.assertEqual(ranked[0]["count"], 2)

    def test_keyword_tools_filter_placeholder_tokens(self) -> None:
        ranked = keyword_rank(["nan nan produto ótimo", "null produto excelente"], limit=3)
        keywords = [item["keyword"] for item in ranked]
        self.assertIn("produto", keywords)
        self.assertNotIn("nan", keywords)
        self.assertNotIn("null", keywords)

    def test_topic_distribution_falls_back_to_rule_based_clusters(self) -> None:
        topics = topic_distribution(
            [
                "entrega atrasada e prazo muito ruim",
                "produto chegou quebrado com baixa qualidade",
                "demorou entrega, nao recebi no prazo",
            ],
            limit=3,
        )

        self.assertEqual(topics[0]["name"], "logistics")
        self.assertEqual(topics[0]["count"], 2)
        self.assertIn({"name": "quality", "count": 1}, topics)

    def test_aggregate_custom_tags_counts_json_array_tags(self) -> None:
        comments = [
            {"custom_tags": '["delivery_delay", "vip"]'},
            {"custom_tags": '["delivery_delay"]'},
            {"custom_tags": None},
        ]

        self.assertEqual(
            aggregate_custom_tags(comments),
            [
                {"name": "delivery_delay", "count": 2},
                {"name": "vip", "count": 1},
            ],
        )

    def test_build_problem_types_prefers_manual_problem_type(self) -> None:
        comments = [
            {"is_negative": 1, "problem_type": "quality", "manual_problem_type": "package"},
            {"is_negative": 1, "problem_type": "logistics", "manual_problem_type": None},
            {"is_negative": 0, "problem_type": "quality", "manual_problem_type": "service"},
        ]

        self.assertEqual(build_problem_types(comments), ["package", "logistics"])

    def test_build_trend_distribution_groups_by_month(self) -> None:
        comments = [
            {"review_time": "2018-05-01 00:00:00", "review_score": 5, "is_negative": 0},
            {"review_time": "2018-05-20 00:00:00", "review_score": 1, "is_negative": 1},
            {"review_time": "2018-06-02 00:00:00", "review_score": 3, "is_negative": 0},
        ]

        self.assertEqual(
            build_trend_distribution(comments, "month"),
            [
                {
                    "timeBucket": "2018-05",
                    "commentCount": 2,
                    "negativeCount": 1,
                    "negativeRate": 0.5,
                    "avgScore": 3.0,
                },
                {
                    "timeBucket": "2018-06",
                    "commentCount": 1,
                    "negativeCount": 0,
                    "negativeRate": 0.0,
                    "avgScore": 3.0,
                },
            ],
        )

    def test_generate_product_compare_keeps_existing_fields_and_adds_invocation_metadata(self) -> None:
        service = AiService()
        chain = FakeStructuredChain(
            AiInvocationResult(
                value=ProductCompareOutput.model_validate(
                    {
                        "compareSummary": "A negative rate is lower.",
                        "advantageAnalysis": "A has better logistics feedback.",
                        "riskAnalysis": "B has more quality complaints.",
                        "operationSuggestions": "Use A as benchmark.",
                    }
                ),
                model_name="deepseek-chat",
                input_tokens=12,
                output_tokens=8,
                total_tokens=20,
                token_usage_estimated=False,
                latency_ms=34,
            )
        )
        service._product_compare_chain = lambda: chain

        result = service.generate_product_compare(
            {
                "leftProductId": "product-a",
                "rightProductId": "product-b",
                "leftAnalysis": {"negativeRate": 0.1},
                "rightAnalysis": {"negativeRate": 0.3},
                "language": "zh-CN",
            }
        )

        self.assertTrue(result["success"])
        self.assertEqual(result["data"]["compareSummary"], "A negative rate is lower.")
        self.assertEqual(result["data"]["operationSuggestions"], "Use A as benchmark.")
        self.assertEqual(result["tokenUsage"], 20)
        self.assertEqual(result["data"]["inputTokens"], 12)
        self.assertEqual(result["data"]["latencyMs"], 34)
        self.assertIn("左侧商品ID：product-a", chain.prompts[0])

    def test_generate_content_returns_structured_chain_metadata(self) -> None:
        service = AiService()
        chain = FakeStructuredChain(
            AiInvocationResult(
                value=ContentGenerationOutput.model_validate(
                    {"generatedContent": "A durable product for everyday use."}
                ),
                model_name="deepseek-chat",
                input_tokens=8,
                output_tokens=7,
                total_tokens=15,
                token_usage_estimated=False,
                latency_ms=23,
            )
        )
        service._content_generation_chain = lambda: chain

        result = service.generate_content(
            {"contentType": "商品详情页", "styleType": "简洁专业", "language": "en-US"}
        )

        self.assertEqual(result["generatedContent"], "A durable product for everyday use.")
        self.assertEqual(result["tokenUsage"], 15)
        self.assertEqual(result["inputTokens"], 8)
        self.assertEqual(result["latencyMs"], 23)
        self.assertIn("文案类型：商品详情页", chain.prompts[0])

    def test_prompt_template_replaces_variables_when_supplied(self) -> None:
        service = AiService()
        chain = FakeNegativeReplyChain("Custom reply")
        service._negative_reply_chain = lambda: chain

        with patch(
            "app.services.ai_service.settings",
            SimpleNamespace(ai_negative_reply_engine="legacy"),
        ):
            result = service.generate_negative_reply(
                {
                    "promptTemplate": "Reply in {language} about {commentContent} for score {reviewScore}.",
                    "promptVariables": {
                        "language": "en-US",
                        "commentContent": "produto quebrado",
                        "reviewScore": 1,
                    },
                }
            )

        self.assertTrue(result["success"])
        self.assertEqual(chain.prompts[0], "Reply in en-US about produto quebrado for score 1.")
        self.assertEqual(result["replyContent"], "Custom reply")
        self.assertEqual(result["tokenUsage"], 24)

    def test_prompt_template_keeps_unknown_variables_for_safe_editing(self) -> None:
        service = AiService()
        rendered = service._render_template("Use {known} and {missing}.", {"known": "value"})

        self.assertEqual(rendered, "Use value and {missing}.")

    def test_generate_negative_reply_uses_comment_context_and_higher_variation(self) -> None:
        service = AiService()
        chain = FakeNegativeReplyChain("尊敬的顾客，很抱歉包装破损影响了体验。")
        service._negative_reply_chain = lambda: chain

        with patch(
            "app.services.ai_service.settings",
            SimpleNamespace(ai_negative_reply_engine="legacy"),
        ):
            result = service.generate_negative_reply(
                {
                    "commentId": 22,
                    "reviewId": "review-22",
                    "productId": "product-a",
                    "reviewScore": 2,
                    "commentTitle": "Entrega atrasada",
                    "commentContent": "produto chegou quebrado",
                    "problemType": "packaging",
                    "toneType": "professional",
                    "language": "zh-CN",
                }
            )

        self.assertTrue(result["success"])
        self.assertIn("produto chegou quebrado", chain.prompts[0])
        self.assertIn("review-22", chain.prompts[0])
        self.assertIn("product-a", chain.prompts[0])
        self.assertIn("2", chain.prompts[0])
        self.assertIn("每条回复都必须针对这条评论单独生成", chain.prompts[0])

    def test_generate_negative_reply_langchain_engine_keeps_java_response_contract(self) -> None:
        service = AiService()
        chain = FakeNegativeReplyChain("Thank you for sharing the delivery issue.", token_usage=31)

        def generate(**kwargs):
            return SimpleNamespace(
                invocation=chain.generate(kwargs["rendered_prompt"]),
                rag_used=False,
                references=[],
            )

        service._rag_reply_service = lambda: SimpleNamespace(generate=generate)

        with patch(
            "app.services.ai_service.settings",
            SimpleNamespace(ai_negative_reply_engine="langchain", ai_model="deepseek-chat"),
        ):
            result = service.generate_negative_reply(
                {
                    "commentId": 23,
                    "commentContent": "The package arrived damaged.",
                    "problemType": "packaging",
                    "language": "en-US",
                }
            )

        self.assertTrue(result["success"])
        self.assertEqual(result["replyContent"], "Thank you for sharing the delivery issue.")
        self.assertEqual(result["modelName"], "deepseek-chat")
        self.assertEqual(result["tokenUsage"], 31)
        self.assertFalse(result["ragUsed"])
        self.assertEqual(result["references"], [])
        self.assertIn("latencyMs", result)
        self.assertEqual(len(chain.prompts), 1)
        self.assertIn("The package arrived damaged.", chain.prompts[0])

    def test_translate_comment_uses_target_language_and_keeps_structured_result(self) -> None:
        service = AiService()
        chain = FakeStructuredChain(
            AiInvocationResult(
                value=CommentTranslationOutput.model_validate(
                    {"translatedContent": "The product arrived broken.", "sourceLanguage": "pt-BR"}
                ),
                model_name="deepseek-chat",
                input_tokens=11,
                output_tokens=9,
                total_tokens=20,
                token_usage_estimated=False,
                latency_ms=17,
            )
        )
        service._comment_translation_chain = lambda: chain

        result = service.translate_comment(
            {
                "commentId": 22,
                "reviewId": "review-22",
                "commentContent": "Produto chegou quebrado",
                "targetLanguage": "en-US",
            }
        )

        self.assertTrue(result["success"])
        self.assertEqual(result["data"]["translatedContent"], "The product arrived broken.")
        self.assertEqual(result["data"]["sourceLanguage"], "pt-BR")
        self.assertEqual(result["data"]["latencyMs"], 17)
        self.assertIn("target language: en-US", chain.prompts[0])
        self.assertIn("Produto chegou quebrado", chain.prompts[0])

    def test_single_csv_comment_rows_require_product_id_and_review_score(self) -> None:
        service = OlistImportService()
        df = pd.DataFrame(
            [
                {
                    "review_id": "review-a",
                    "product_id": "product-a",
                    "seller_id": "seller-a",
                    "review_score": 1,
                    "review_content": "produto chegou quebrado",
                }
            ]
        )

        rows = service._build_comment_rows(df)

        self.assertEqual(rows[0]["review_id"], "review-a")
        self.assertEqual(rows[0]["product_id"], "product-a")
        self.assertEqual(rows[0]["seller_id"], "seller-a")
        self.assertEqual(rows[0]["sentiment"], "negative")
        self.assertEqual(rows[0]["problem_type"], "quality")

        with self.assertRaisesRegex(ValueError, "product_id.*review_score"):
            service._build_comment_rows(pd.DataFrame([{"review_content": "sem identificador"}]))


if __name__ == "__main__":
    unittest.main()
